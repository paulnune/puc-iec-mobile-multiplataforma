package com.puciec.eleicoes.data.cache

import com.puciec.eleicoes.data.EleicoesJson
import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.DataStatus
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Cache offline (#12): decorator da porta [EleicoesDataSource]. Grava em disco a última resposta boa
 * de cada consulta e a devolve quando a rede ou a API falham. Repositórios e telas não mudam.
 *
 * Uma resposta salva há menos de [freshFor] volta sem chamar a fonte, para respeitar o limite de
 * 60 req/min mesmo depois de fechar e abrir o app (AD-007). [now] é o relógio em milissegundos
 * (injetado para os testes controlarem o tempo).
 */
class CachingEleicoesDataSource(
    private val delegate: EleicoesDataSource,
    private val store: CacheStore,
    private val now: () -> Long,
    private val freshFor: Duration = 5.minutes,
    private val json: Json = EleicoesJson,
) : EleicoesDataSource {

    private val _status = MutableStateFlow<DataStatus>(DataStatus.Live)

    /** De onde veio a última resposta: ao vivo, ou do disco por causa de uma falha (com a hora salva). */
    val status: StateFlow<DataStatus> = _status.asStateFlow()

    override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> =
        cached("listCandidates|${filter.key()}|$page|$limit", PageDto.serializer(CandidateDto.serializer())) {
            delegate.listCandidates(filter, page, limit)
        }

    override suspend fun getCandidate(id: String): CandidateDto =
        cached("getCandidate|$id", CandidateDto.serializer()) { delegate.getCandidate(id) }

    override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> =
        cached("listCandidateAssets|$candidateId|$page|$limit", PageDto.serializer(AssetDto.serializer())) {
            delegate.listCandidateAssets(candidateId, page, limit)
        }

    override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> =
        cached("getCandidateFilings|$candidateId", ListSerializer(FilingDto.serializer())) {
            delegate.getCandidateFilings(candidateId)
        }

    override suspend fun listWealthChanges(
        sort: WealthSort,
        filter: CandidateFilter,
        page: Int,
        limit: Int,
    ): PageDto<WealthChangeDto> =
        cached(
            "listWealthChanges|${sort.apiValue}|${filter.key()}|$page|$limit",
            PageDto.serializer(WealthChangeDto.serializer()),
        ) { delegate.listWealthChanges(sort, filter, page, limit) }

    override suspend fun listPlaceResults(
        office: Office,
        scope: PlaceScope,
        state: String?,
        round: Int?,
    ): List<PlaceResultDto> =
        cached(
            "listPlaceResults|${office.apiValue}|${scope.apiValue}|${state.orEmpty()}|${round ?: ""}",
            ListSerializer(PlaceResultDto.serializer()),
        ) { delegate.listPlaceResults(office, scope, state, round) }

    override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto =
        cached(
            "getPlaceResult|$code|${office.apiValue}|${scope.apiValue}|${round ?: ""}",
            PlaceResultDto.serializer(),
        ) { delegate.getPlaceResult(code, office, scope, round) }

    override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> =
        cached("listParties|${query.orEmpty()}|$page|$limit", PageDto.serializer(PartyDto.serializer())) {
            delegate.listParties(query, page, limit)
        }

    override suspend fun getParty(id: String): PartyDto =
        cached("getParty|$id", PartyDto.serializer()) { delegate.getParty(id) }

    /**
     * Salvo há menos de [freshFor]: devolve sem chamar a fonte e sem mexer no status. Senão chama a
     * fonte, grava o sucesso e, nas falhas da AD-006, devolve o salvo de qualquer idade (AD-008).
     * Um arquivo ilegível conta como ausente, e uma falha ao gravar não derruba a resposta.
     */
    private suspend fun <T> cached(key: String, serializer: KSerializer<T>, load: suspend () -> T): T {
        val saved = readSaved(key, serializer)
        // Data no futuro (relógio do aparelho mudou) não conta como recente.
        val age = saved?.let { now() - it.savedAtEpochMillis }
        if (saved != null && age != null && age >= 0 && age < freshFor.inWholeMilliseconds) return saved.value

        val value = try {
            load()
        } catch (e: AppError) {
            if (!e.usesCache() || saved == null) throw e
            _status.value = DataStatus.Cached(saved.savedAtEpochMillis)
            return saved.value
        }
        writeQuietly(key, CachedResponse(json.encodeToString(serializer, value), now()))
        _status.value = DataStatus.Live
        return value
    }

    private class Saved<T>(val value: T, val savedAtEpochMillis: Long)

    private suspend fun <T> readSaved(key: String, serializer: KSerializer<T>): Saved<T>? {
        val stored = try {
            store.read(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return null
        return try {
            Saved(json.decodeFromString(serializer, stored.json), stored.savedAtEpochMillis)
        } catch (e: IllegalArgumentException) {
            // SerializationException herda de IllegalArgumentException: arquivo corrompido ou de outro formato.
            null
        }
    }

    private suspend fun writeQuietly(key: String, response: CachedResponse) {
        try {
            store.write(key, response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sem espaço ou sem permissão: a tela segue com a resposta ao vivo.
        }
    }
}

/** Falhas em que a resposta salva vale mais que o erro (AD-006). Não encontrado e chave inválida não entram. */
private fun AppError.usesCache(): Boolean =
    this is AppError.NoConnection || this is AppError.Server || this is AppError.RateLimited || this is AppError.Unexpected

/** Chave estável do filtro: os conjuntos vão ordenados, então a ordem de inserção não importa. */
private fun CandidateFilter.key(): String = listOf(
    offices.map { it.apiValue }.sorted().joinToString(","),
    states.sorted().joinToString(","),
    partyIds.sorted().joinToString(","),
    gender?.apiValue.orEmpty(),
    outcome?.name.orEmpty(),
    query.orEmpty(),
).joinToString(";")
