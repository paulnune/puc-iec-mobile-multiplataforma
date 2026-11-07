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
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Cache offline (#12): decorator da porta [EleicoesDataSource]. Grava em disco a última resposta boa
 * de cada consulta e a devolve quando a rede ou a API falham. Repositórios e telas não mudam.
 *
 * [now] é o relógio em milissegundos (injetado para os testes controlarem o tempo).
 */
class CachingEleicoesDataSource(
    private val delegate: EleicoesDataSource,
    private val store: CacheStore,
    private val now: () -> Long,
    private val json: Json = EleicoesJson,
) : EleicoesDataSource {

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

    /** Chama a fonte; grava o sucesso; nas falhas da AD-006 devolve o salvo, se houver. */
    private suspend fun <T> cached(key: String, serializer: KSerializer<T>, load: suspend () -> T): T {
        val saved = store.read(key)
        val value = try {
            load()
        } catch (e: AppError) {
            if (e.usesCache() && saved != null) return json.decodeFromString(serializer, saved.json)
            throw e
        }
        store.write(key, CachedResponse(json.encodeToString(serializer, value), now()))
        return value
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
