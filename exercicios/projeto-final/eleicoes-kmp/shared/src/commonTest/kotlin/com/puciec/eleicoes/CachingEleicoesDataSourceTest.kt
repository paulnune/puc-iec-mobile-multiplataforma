package com.puciec.eleicoes

import com.puciec.eleicoes.data.EleicoesJson
import com.puciec.eleicoes.data.cache.CacheStore
import com.puciec.eleicoes.data.cache.CachedResponse
import com.puciec.eleicoes.data.cache.CachingEleicoesDataSource
import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PaginationDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.VoteDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.DataStatus
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer

/** Cache offline (#12): o decorator da porta contra uma fonte falsa e um armazenamento em memória. */
class CachingEleicoesDataSourceTest {
    private class MemoryStore : CacheStore {
        val entries = mutableMapOf<String, CachedResponse>()
        var failWrites = false

        override suspend fun read(key: String): CachedResponse? = entries[key]

        override suspend fun write(key: String, response: CachedResponse) {
            if (failWrites) throw IllegalStateException("disco cheio")
            entries[key] = response
        }
    }

    /** Fonte falsa: cada resposta carrega a [version], para distinguir a resposta nova da salva. */
    private class FakeSource : EleicoesDataSource {
        var error: Throwable? = null
        var calls = 0
        var version = 1

        private fun <T> answer(value: () -> T): T {
            calls++
            error?.let { throw it }
            return value()
        }

        private fun place(code: String) = PlaceResultDto(
            office = "president", scope = "uf", state = code, code = code,
            leader = VoteDto(votes = 100L * version, ballotName = "LULA v$version", partyAcronym = "PT"),
        )

        override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int) = answer {
            PageDto(listOf(CandidateDto(id = "c$page-v$version", ballotName = "LULA")), PaginationDto(page = page, total = 1))
        }
        override suspend fun getCandidate(id: String) = answer { CandidateDto(id = id, ballotName = "v$version") }
        override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int) = answer {
            PageDto(listOf(AssetDto(id = "a-v$version", value = 10.0 * version)))
        }
        override suspend fun getCandidateFilings(candidateId: String) = answer { listOf(FilingDto(id = "f-v$version", year = 2022)) }
        override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int) = answer {
            PageDto(listOf(WealthChangeDto(id = "w-v$version", delta = 1.0 * version)))
        }
        override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?) = answer {
            listOf(place("SP"), place("BA"))
        }
        override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?) = answer { place(code) }
        override suspend fun listParties(query: String?, page: Int, limit: Int) = answer {
            PageDto(listOf(PartyDto(id = "p-v$version", number = 13, acronym = "PT")))
        }
        override suspend fun getParty(id: String) = answer { PartyDto(id = id, acronym = "v$version") }
    }

    private val day = 24 * 60 * 60 * 1000L
    private val start = 1_762_500_000_000L

    private val source = FakeSource()
    private val store = MemoryStore()
    private var clock = start
    private val cache = CachingEleicoesDataSource(delegate = source, store = store, now = { clock })

    private val fallbackErrors = listOf(
        AppError.NoConnection,
        AppError.Server("HTTP 500"),
        AppError.RateLimited(retryAfterSeconds = 30),
        AppError.Unexpected("Formato inesperado"),
    )

    /** Chama as 9 operações da porta, na mesma ordem, com os mesmos argumentos. */
    private suspend fun callAll(): List<Any> = listOf(
        cache.listCandidates(CandidateFilter(offices = setOf(Office.PRESIDENT)), page = 1, limit = 30),
        cache.getCandidate("c1"),
        cache.listCandidateAssets("c1", page = 1, limit = 50),
        cache.getCandidateFilings("c1"),
        cache.listWealthChanges(WealthSort.GAIN, CandidateFilter(), page = 1, limit = 30),
        cache.listPlaceResults(Office.PRESIDENT, PlaceScope.UF, state = null, round = 1),
        cache.getPlaceResult("SP", Office.GOVERNOR, PlaceScope.UF, round = 1),
        cache.listParties(query = null, page = 1, limit = 50),
        cache.getParty("p13"),
    )

    @Test
    fun `resposta ao vivo e gravada com a hora`() = runTest {
        val live = cache.listPlaceResults(Office.PRESIDENT, PlaceScope.BRASIL, state = null, round = 1)

        assertEquals(1, store.entries.size)
        val saved = store.entries.values.single()
        assertEquals(start, saved.savedAtEpochMillis)
        assertEquals(live, EleicoesJson.decodeFromString(ListSerializer(PlaceResultDto.serializer()), saved.json))
        assertEquals("LULA v1", live.first().leader?.ballotName)
    }

    @Test
    fun `com a fonte falhando devolve o cache`() = runTest {
        for (error in fallbackErrors) {
            val store = MemoryStore()
            val source = FakeSource()
            var now = start
            val cache = CachingEleicoesDataSource(delegate = source, store = store, now = { now })
            val live = cache.getPlaceResult("SP", Office.GOVERNOR, PlaceScope.UF, round = 1)

            now = start + 30 * day
            source.version = 2
            source.error = error

            val offline = cache.getPlaceResult("SP", Office.GOVERNOR, PlaceScope.UF, round = 1)
            assertEquals(live, offline, "fallback com $error")
            assertEquals("LULA v1", offline.leader?.ballotName)
        }
    }

    @Test
    fun `sem cache devolve o erro da fonte`() = runTest {
        for (error in fallbackErrors) {
            source.error = error
            val thrown = assertFails { cache.getParty("p13") }
            assertEquals(error, thrown)
        }
        assertEquals(0, store.entries.size)
    }

    @Test
    fun `nao encontrado e chave invalida nao usam o cache`() = runTest {
        cache.getCandidate("c1")
        clock = start + 30 * day
        for (error in listOf(AppError.NotFound, AppError.InvalidApiKey, AppError.MissingApiKey)) {
            source.error = error
            val thrown = assertFails { cache.getCandidate("c1") }
            assertEquals(error, thrown)
        }
    }

    @Test
    fun `todas as operacoes usam o cache`() = runTest {
        val live = callAll()
        assertEquals(9, store.entries.size, "uma resposta salva por operação")

        clock = start + 30 * day
        source.version = 2
        source.error = AppError.NoConnection

        val offline = callAll()
        live.zip(offline).forEachIndexed { index, (expected, actual) ->
            assertEquals(expected, actual, "operação $index")
        }
    }

    @Test
    fun `argumentos diferentes usam chaves diferentes`() = runTest {
        val filter = CandidateFilter(offices = setOf(Office.PRESIDENT, Office.GOVERNOR), states = setOf("SP", "BA"))
        cache.listCandidates(filter, page = 1, limit = 30)
        cache.listCandidates(filter, page = 2, limit = 30)
        assertEquals(2, store.entries.size, "página 1 e página 2")

        cache.listPlaceResults(Office.PRESIDENT, PlaceScope.UF, state = null, round = 1)
        cache.listPlaceResults(Office.GOVERNOR, PlaceScope.UF, state = null, round = 1)
        assertEquals(4, store.entries.size, "presidente e governador")

        cache.getCandidate("x")
        cache.getParty("x")
        assertEquals(6, store.entries.size, "mesmo id em operações diferentes")

        // O mesmo filtro com os conjuntos em outra ordem é a mesma consulta: não cria arquivo novo.
        val reordered = CandidateFilter(offices = setOf(Office.GOVERNOR, Office.PRESIDENT), states = setOf("BA", "SP"))
        assertNotEquals(filter.offices.toList(), reordered.offices.toList())
        cache.listCandidates(reordered, page = 1, limit = 30)
        assertEquals(6, store.entries.size, "conjuntos em outra ordem")
    }

    @Test
    fun `status comeca Live`() {
        assertEquals(DataStatus.Live, cache.status.value)
    }

    @Test
    fun `fallback publica Cached com a hora salva`() = runTest {
        cache.getParty("p13")
        clock = start + 2 * day
        source.error = AppError.Server("HTTP 500")

        cache.getParty("p13")

        assertEquals(DataStatus.Cached(savedAtEpochMillis = start), cache.status.value)
    }

    @Test
    fun `resposta ao vivo volta a publicar Live`() = runTest {
        cache.getParty("p13")
        clock = start + 2 * day
        source.error = AppError.NoConnection
        cache.getParty("p13")
        assertEquals(DataStatus.Cached(savedAtEpochMillis = start), cache.status.value)

        source.error = null
        clock += day
        cache.getCandidate("outra-consulta")

        assertEquals(DataStatus.Live, cache.status.value)
    }
}
