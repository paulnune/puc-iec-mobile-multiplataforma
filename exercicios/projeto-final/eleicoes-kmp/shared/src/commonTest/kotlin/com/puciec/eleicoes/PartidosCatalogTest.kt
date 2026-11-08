package com.puciec.eleicoes

import com.puciec.eleicoes.data.cache.CacheStore
import com.puciec.eleicoes.data.cache.CachedResponse
import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PaginationDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.partidos.PartidosCatalog
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest

/** Cache de 1 dia da aba Partidos (#8, revisão 8): relógio controlado e disco em memória, nada sai para a rede. */
class PartidosCatalogTest {

    private class MemoryStore : CacheStore {
        val files = mutableMapOf<String, CachedResponse>()
        override suspend fun read(key: String) = files[key]
        override suspend fun write(key: String, response: CachedResponse) {
            files[key] = response
        }
    }

    /** Fonte falsa: 2 partidos e candidatos por partido; conta chamadas e pode falhar. */
    private class FakeSource : EleicoesDataSource {
        var partyCalls = 0
        var candidateCalls = mutableMapOf<String, Int>()
        var fail: AppError? = null
        var parties = listOf(
            PartyDto(id = "pl", number = 22, acronym = "PL", name = "PARTIDO LIBERAL"),
            PartyDto(id = "pt", number = 13, acronym = "PT", name = "PARTIDO DOS TRABALHADORES"),
        )
        var totals = mutableMapOf("pl" to 1647, "pt" to 1103)

        override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> {
            partyCalls++
            fail?.let { throw it }
            return PageDto(parties, PaginationDto(page = 1, limit = limit, total = parties.size, totalPages = 1))
        }

        override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> {
            val id = filter.partyIds.single()
            candidateCalls[id] = (candidateCalls[id] ?: 0) + 1
            fail?.let { throw it }
            val item = CandidateDto(id = "$id-c1", partyId = id, ballotName = "CANDIDATO $id", office = "senator", officeLabel = "SENADOR")
            return PageDto(listOf(item), PaginationDto(page = page, limit = limit, total = totals[id], totalPages = 10))
        }

        override suspend fun getParty(id: String): PartyDto = TODO()
        override suspend fun getCandidate(id: String): CandidateDto = TODO()
        override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> = TODO()
        override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> = TODO()
        override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> = TODO()
        override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> = TODO()
        override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto = TODO()
    }

    private val source = FakeSource()
    private val store = MemoryStore()
    private var clock = 1_000_000L
    private val catalog = PartidosCatalog(source, store, now = { clock })

    // --- Lista de partidos ---

    @Test
    fun `primeira vez busca na API e grava no aparelho`() = runTest {
        val result = catalog.parties()

        assertEquals(listOf("PT", "PL"), result.value.map { it.acronym })
        assertEquals(1, source.partyCalls)
        assertEquals(clock, result.savedAtEpochMillis)
        assertFalse(result.fromFallback)
        assertTrue(store.files.isNotEmpty())
    }

    @Test
    fun `dentro de 1 dia nao chama a API`() = runTest {
        catalog.parties()
        clock += 23.hours.inWholeMilliseconds

        catalog.parties()

        assertEquals(1, source.partyCalls)
    }

    @Test
    fun `depois de 1 dia busca de novo e regrava`() = runTest {
        catalog.parties()
        clock += 1.days.inWholeMilliseconds + 1
        source.parties = source.parties + PartyDto(id = "novo", number = 99, acronym = "PNF", name = "PARTIDO NOVO FUTURO")

        val result = catalog.parties()

        assertEquals(2, source.partyCalls)
        assertEquals(listOf("PT", "PL", "PNF"), result.value.map { it.acronym })
        assertEquals(clock, result.savedAtEpochMillis)
    }

    @Test
    fun `force busca mesmo com o dado fresco`() = runTest {
        catalog.parties()

        catalog.parties(force = true)

        assertEquals(2, source.partyCalls)
    }

    @Test
    fun `sobrevive a reabrir o app porque le do aparelho`() = runTest {
        catalog.parties()
        // Um catálogo novo (app reaberto) com o mesmo disco: não chama a API.
        val reaberto = PartidosCatalog(source, store, now = { clock })

        assertEquals(listOf("PT", "PL"), reaberto.parties().value.map { it.acronym })
        assertEquals(1, source.partyCalls)
    }

    @Test
    fun `falha da API devolve o salvo mesmo vencido`() = runTest {
        val saved = catalog.parties().savedAtEpochMillis
        clock += 3.days.inWholeMilliseconds
        source.fail = AppError.NoConnection

        val result = catalog.parties(force = true)

        assertEquals(listOf("PT", "PL"), result.value.map { it.acronym })
        assertTrue(result.fromFallback)
        assertEquals(saved, result.savedAtEpochMillis)
    }

    @Test
    fun `sem nada salvo a falha da API propaga`() = runTest {
        source.fail = AppError.NoConnection

        assertFailsWith<AppError.NoConnection> { catalog.parties() }
    }

    @Test
    fun `arquivo corrompido conta como ausente`() = runTest {
        catalog.parties()
        store.files.keys.forEach { store.files[it] = CachedResponse("{isso nao e json", clock) }

        catalog.parties()

        assertEquals(2, source.partyCalls)
    }

    // --- 1ª página de candidatos de cada partido ---

    @Test
    fun `primeira pagina do partido traz o total e fica salva por 1 dia`() = runTest {
        val first = catalog.firstPage("pl")
        clock += 12.hours.inWholeMilliseconds
        val again = catalog.firstPage("pl")

        assertEquals(1647, first.value.total)
        assertEquals("PL", first.value.items.single().partyAcronym)
        assertEquals(first.value, again.value)
        assertEquals(1, source.candidateCalls["pl"])
    }

    @Test
    fun `validade e independente por partido`() = runTest {
        catalog.firstPage("pl")
        clock += 20.hours.inWholeMilliseconds
        catalog.firstPage("pt")
        clock += 5.hours.inWholeMilliseconds // PL com 25 h, PT com 5 h

        catalog.firstPage("pl")
        catalog.firstPage("pt")

        assertEquals(2, source.candidateCalls["pl"])
        assertEquals(1, source.candidateCalls["pt"])
    }

    @Test
    fun `force da pagina so busca aquele partido`() = runTest {
        catalog.firstPage("pl")
        catalog.firstPage("pt")
        source.totals["pl"] = 1700

        val refreshed = catalog.firstPage("pl", force = true)

        assertEquals(1700, refreshed.value.total)
        assertEquals(2, source.candidateCalls["pl"])
        assertEquals(1, source.candidateCalls["pt"])
    }

    @Test
    fun `pagina do partido sem rede usa a salva`() = runTest {
        catalog.firstPage("pl")
        source.fail = AppError.NoConnection
        clock += 2.days.inWholeMilliseconds

        val result = catalog.firstPage("pl")

        assertEquals(1647, result.value.total)
        assertTrue(result.fromFallback)
    }
}
