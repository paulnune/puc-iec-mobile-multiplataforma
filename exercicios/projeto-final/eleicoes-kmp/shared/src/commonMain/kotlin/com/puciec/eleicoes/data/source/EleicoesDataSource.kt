package com.puciec.eleicoes.data.source

import com.puciec.eleicoes.ApiConfig
import com.puciec.eleicoes.data.createHttpClient
import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.source.mcp.McpEleicoesDataSource
import com.puciec.eleicoes.data.source.rest.RestEleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort

/**
 * Porta de dados (arquitetura hexagonal): o resto do app só conhece esta interface.
 *
 * Implementações: [McpEleicoesDataSource] (padrão) e [RestEleicoesDataSource]. Em 06/11 a REST
 * ignorava os filtros e devolvia 500 nas rotas com id; o MCP, com a mesma chave, funcionava.
 * Os retornos são DTOs serializáveis, então um decorator de cache (#12) guarda a resposta como veio.
 * Erros chegam como [com.puciec.eleicoes.domain.model.AppError].
 */
interface EleicoesDataSource {
    suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto>

    suspend fun getCandidate(id: String): CandidateDto

    suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto>

    suspend fun getCandidateFilings(candidateId: String): List<FilingDto>

    /** Quebrada no servidor (erro de SQL) em 07/11: o #7 calcula a variação a partir das filings. */
    suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto>

    suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto>

    suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto

    suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto>

    suspend fun getParty(id: String): PartyDto
}

enum class Transport { MCP, REST }

/** Fábrica usada pelo app. Troque o [transport] para testar a REST quando ela voltar. */
fun createEleicoesDataSource(
    apiKey: String = ApiConfig.apiKey,
    transport: Transport = Transport.MCP,
): EleicoesDataSource {
    val client = createHttpClient()
    return when (transport) {
        Transport.MCP -> McpEleicoesDataSource(client, apiKey)
        Transport.REST -> RestEleicoesDataSource(client, apiKey)
    }
}
