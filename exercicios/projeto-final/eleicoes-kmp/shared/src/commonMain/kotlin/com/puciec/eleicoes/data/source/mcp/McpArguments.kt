package com.puciec.eleicoes.data.source.mcp

import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Argumentos de cada tool, no formato do `inputSchema` publicado pelo servidor:
 * `office`, `state`, `partyId` e `gender` são listas; `outcome`, `q` e `sort` são texto.
 */
internal object McpArguments {
    fun candidates(filter: CandidateFilter, page: Int, limit: Int): JsonObject = buildJsonObject {
        put("page", page)
        put("limit", limit)
        putFilter(filter)
    }

    fun wealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): JsonObject = buildJsonObject {
        put("page", page)
        put("limit", limit)
        put("sort", sort.apiValue)
        putFilter(filter)
    }

    fun id(id: String): JsonObject = buildJsonObject { put("id", id) }

    fun assets(candidateId: String, page: Int, limit: Int): JsonObject = buildJsonObject {
        put("id", candidateId)
        put("page", page)
        put("limit", limit)
    }

    fun placeResults(office: Office, scope: PlaceScope, state: String?, round: Int?): JsonObject = buildJsonObject {
        put("office", office.apiValue)
        put("scope", scope.apiValue)
        state?.let { put("state", it) }
        round?.let { put("round", it) }
    }

    fun placeResult(code: String, office: Office, scope: PlaceScope, round: Int?): JsonObject = buildJsonObject {
        put("code", code)
        put("office", office.apiValue)
        put("scope", scope.apiValue)
        round?.let { put("round", it) }
    }

    fun parties(query: String?, page: Int, limit: Int): JsonObject = buildJsonObject {
        put("page", page)
        put("limit", limit)
        query?.trim()?.takeIf { it.isNotEmpty() }?.let { put("q", it) }
    }

    private fun JsonObjectBuilder.putFilter(filter: CandidateFilter) {
        if (filter.offices.isNotEmpty()) {
            putJsonArray("office") { filter.offices.sortedBy { it.ordinal }.forEach { add(it.apiValue) } }
        }
        if (filter.states.isNotEmpty()) {
            putJsonArray("state") { filter.states.sorted().forEach { add(it) } }
        }
        if (filter.partyIds.isNotEmpty()) {
            putJsonArray("partyId") { filter.partyIds.sorted().forEach { add(it) } }
        }
        filter.gender?.let { gender -> putJsonArray("gender") { add(gender.apiValue) } }
        filter.outcome?.apiValue?.let { put("outcome", it) }
        filter.query?.trim()?.takeIf { it.isNotEmpty() }?.let { put("q", it) }
    }
}
