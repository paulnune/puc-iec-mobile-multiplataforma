package com.puciec.eleicoes.data.partidos

import com.puciec.eleicoes.data.EleicoesJson
import com.puciec.eleicoes.data.cache.CacheStore
import com.puciec.eleicoes.data.cache.CachedResponse
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.mapper.toDomain
import com.puciec.eleicoes.data.mapper.toPage
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Page
import com.puciec.eleicoes.domain.model.Party
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Dados da aba Partidos (#8) guardados no aparelho por [ttl] (1 dia): a lista de partidos e a 1ª página de
 * candidatos de cada um (que traz o total de candidaturas). São só 30 partidos e o resultado da eleição é final,
 * então a aba pode abrir sem rede e sem gastar o limite de 60 req/min da chave.
 *
 * - Dentro do prazo: devolve do aparelho, sem chamar a API.
 * - Vencido ou `force = true` (botão de recarregar): busca na API e regrava.
 * - API ou rede falhou: devolve o que estiver salvo, de qualquer idade ([Cached.fromFallback]).
 *
 * Use com a fonte **crua** (sem o cache de 5 min do #12): o recarregar precisa buscar de verdade.
 * Arquivo novo e independente: não altera os repositórios nem o cache em disco do resto do app.
 */
class PartidosCatalog(
    private val source: EleicoesDataSource,
    private val store: CacheStore,
    private val now: () -> Long,
    private val ttl: Duration = 1.days,
    private val pageSize: Int = PAGE_SIZE,
    private val json: Json = EleicoesJson,
) {
    /** Um dado com a hora em que foi buscado na API; [fromFallback] = veio do aparelho porque a API falhou. */
    data class Cached<T>(val value: T, val savedAtEpochMillis: Long, val fromFallback: Boolean)

    private val partiesLock = Mutex()
    private val partiesSerializer = ListSerializer(PartyDto.serializer())
    private val pageSerializer = PageDto.serializer(CandidateDto.serializer())

    /** Todos os partidos como a API manda, na ordem do número. */
    suspend fun parties(force: Boolean = false): Cached<List<Party>> {
        val saved = partiesLock.withLock { cached(PARTIES_KEY, partiesSerializer, force) { fetchAllParties() } }
        return Cached(saved.value.map { it.toDomain() }.sortedBy { it.number }, saved.savedAtEpochMillis, saved.fromFallback)
    }

    /**
     * 1ª página dos candidatos do partido (do maior para o menor patrimônio), com o `total` de candidaturas.
     * É a mesma consulta que a tela do partido faz no chip "Todos".
     */
    suspend fun firstPage(partyId: String, force: Boolean = false): Cached<Page<Candidate>> {
        val saved = cached("$PAGE_KEY_PREFIX$partyId", pageSerializer, force) {
            source.listCandidates(CandidateFilter(partyIds = setOf(partyId)), page = 1, limit = pageSize)
        }
        val acronyms = acronymsById()
        return Cached(saved.value.toPage(1) { it.toDomain(acronyms) }, saved.savedAtEpochMillis, saved.fromFallback)
    }

    /** Sigla por id para os cards; se nem a lista existir, os cards saem sem sigla em vez de dar erro. */
    private suspend fun acronymsById(): Map<String, String> =
        try {
            parties().value.associate { it.id to it.acronym }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }

    private suspend fun fetchAllParties(): List<PartyDto> {
        val first = source.listParties(query = null, page = 1, limit = PARTIES_PAGE_SIZE)
        val others = (2..(first.pagination?.totalPages ?: 1)).flatMap { page ->
            source.listParties(query = null, page = page, limit = PARTIES_PAGE_SIZE).data
        }
        return first.data + others
    }

    private suspend fun <T> cached(key: String, serializer: KSerializer<T>, force: Boolean, load: suspend () -> T): Cached<T> {
        val saved = read(key, serializer)
        val age = saved?.let { now() - it.savedAtEpochMillis }
        // Relógio do aparelho voltou no tempo (idade negativa): não confia, busca de novo.
        if (!force && saved != null && age != null && age >= 0 && age < ttl.inWholeMilliseconds) return saved

        val value = try {
            load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (saved == null) throw e
            return saved.copy(fromFallback = true)
        }
        val savedAt = now()
        try {
            store.write(key, CachedResponse(json.encodeToString(serializer, value), savedAt))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sem espaço ou disco ilegível: o dado novo ainda vale para esta sessão.
        }
        return Cached(value, savedAt, fromFallback = false)
    }

    /** O salvo para [key], ou nulo se não houver ou se o arquivo estiver ilegível. */
    private suspend fun <T> read(key: String, serializer: KSerializer<T>): Cached<T>? {
        val response = try {
            store.read(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return null
        return try {
            Cached(json.decodeFromString(serializer, response.json), response.savedAtEpochMillis, fromFallback = false)
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val PARTIES_KEY = "partidos|lista"
        const val PAGE_KEY_PREFIX = "partidos|pagina1|"
        const val PARTIES_PAGE_SIZE = 50
        const val PAGE_SIZE = 30
    }
}
