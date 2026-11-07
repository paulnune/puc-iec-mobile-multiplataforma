package com.puciec.eleicoes.data.source.rest

import com.puciec.eleicoes.ApiConfig
import com.puciec.eleicoes.data.EleicoesJson
import com.puciec.eleicoes.data.checkHttpStatus
import com.puciec.eleicoes.data.decodeOrThrow
import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.networkCall
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Segundo adapter, pelo contrato OpenAPI (https://eleicoes.dev/docs/api). Fica pronto para quando
 * a REST voltar: em 06/11 ela ignorava os parâmetros e devolvia 500 nas rotas com id.
 */
class RestEleicoesDataSource(
    private val client: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = ApiConfig.REST_BASE_URL,
    private val json: Json = EleicoesJson,
) : EleicoesDataSource {
    override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> =
        get("/candidates", PageDto.serializer(CandidateDto.serializer())) {
            pageParams(page, limit)
            filterParams(filter)
        }

    override suspend fun getCandidate(id: String): CandidateDto =
        get("/candidates/$id", CandidateDto.serializer())

    override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> =
        get("/candidates/$candidateId/assets", PageDto.serializer(AssetDto.serializer())) { pageParams(page, limit) }

    override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> =
        get("/candidates/$candidateId/filings", PageDto.serializer(FilingDto.serializer())).data

    override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> =
        get("/candidates/wealth-changes", PageDto.serializer(WealthChangeDto.serializer())) {
            pageParams(page, limit)
            parameter("sort", sort.apiValue)
            filterParams(filter)
        }

    override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> =
        get("/results", PageDto.serializer(PlaceResultDto.serializer())) {
            parameter("office", office.apiValue)
            parameter("scope", scope.apiValue)
            state?.let { parameter("state", it) }
            round?.let { parameter("round", it) }
        }.data

    override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto =
        get("/results/$code", PlaceResultDto.serializer()) {
            parameter("office", office.apiValue)
            parameter("scope", scope.apiValue)
            round?.let { parameter("round", it) }
        }

    override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> =
        get("/parties", PageDto.serializer(PartyDto.serializer())) {
            pageParams(page, limit)
            query?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("q", it) }
        }

    override suspend fun getParty(id: String): PartyDto =
        get("/parties/$id", PartyDto.serializer())

    private suspend fun <T> get(
        path: String,
        serializer: KSerializer<T>,
        params: HttpRequestBuilder.() -> Unit = {},
    ): T {
        if (apiKey.isBlank()) throw AppError.MissingApiKey
        val response = networkCall {
            client.get(baseUrl + path) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                params()
            }
        }
        checkHttpStatus(response.status.value, response.headers[HttpHeaders.RetryAfter])
        val body = networkCall { response.bodyAsText() }
        return decodeOrThrow(json, serializer, body, path)
    }

    private fun HttpRequestBuilder.pageParams(page: Int, limit: Int) {
        parameter("page", page)
        parameter("limit", limit)
    }

    /** Filtros múltiplos vão como chave repetida: ?office=governor&office=senator. */
    private fun HttpRequestBuilder.filterParams(filter: CandidateFilter) {
        filter.offices.forEach { parameter("office", it.apiValue) }
        filter.states.forEach { parameter("state", it) }
        filter.partyIds.forEach { parameter("partyId", it) }
        filter.gender?.let { parameter("gender", it.apiValue) }
        filter.outcome?.apiValue?.let { parameter("outcome", it) }
        filter.query?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("q", it) }
    }
}
