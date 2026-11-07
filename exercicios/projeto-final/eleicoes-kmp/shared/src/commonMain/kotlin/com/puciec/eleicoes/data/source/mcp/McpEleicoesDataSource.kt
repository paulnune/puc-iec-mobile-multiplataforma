package com.puciec.eleicoes.data.source.mcp

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
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Adapter padrão: chama as tools do servidor MCP do eleicoes.dev por HTTP (JSON-RPC).
 * Mesma chave e mesmos dados da REST, que estava quebrada na semana da entrega.
 */
class McpEleicoesDataSource(
    private val client: HttpClient,
    private val apiKey: String,
    private val endpoint: String = ApiConfig.MCP_ENDPOINT,
    private val json: Json = EleicoesJson,
) : EleicoesDataSource {
    private var nextRequestId = 0

    override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> =
        call("list_candidates", McpArguments.candidates(filter, page, limit), PageDto.serializer(CandidateDto.serializer()))

    override suspend fun getCandidate(id: String): CandidateDto =
        call("get_candidate", McpArguments.id(id), CandidateDto.serializer())

    override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> =
        call("list_candidate_assets", McpArguments.assets(candidateId, page, limit), PageDto.serializer(AssetDto.serializer()))

    override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> =
        call("get_candidate_filings", McpArguments.id(candidateId), PageDto.serializer(FilingDto.serializer())).data

    override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> =
        call("list_wealth_changes", McpArguments.wealthChanges(sort, filter, page, limit), PageDto.serializer(WealthChangeDto.serializer()))

    override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> =
        call("list_place_results", McpArguments.placeResults(office, scope, state, round), PageDto.serializer(PlaceResultDto.serializer())).data

    override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto =
        call("get_place_result", McpArguments.placeResult(code, office, scope, round), PlaceResultDto.serializer())

    override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> =
        call("list_parties", McpArguments.parties(query, page, limit), PageDto.serializer(PartyDto.serializer()))

    override suspend fun getParty(id: String): PartyDto =
        call("get_party", McpArguments.id(id), PartyDto.serializer())

    private suspend fun <T> call(tool: String, arguments: JsonObject, serializer: KSerializer<T>): T {
        if (apiKey.isBlank()) throw AppError.MissingApiKey
        val requestBody = McpProtocol.toolCall(++nextRequestId, tool, arguments).toString()
        val response = networkCall {
            client.post(endpoint) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header(HttpHeaders.Accept, "application/json, text/event-stream")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
        checkHttpStatus(response.status.value, response.headers[HttpHeaders.RetryAfter])
        val body = networkCall { response.bodyAsText() }
        return decodeOrThrow(json, serializer, McpProtocol.toolPayload(body), tool)
    }
}
