package com.puciec.eleicoes.data.source.mcp

import com.puciec.eleicoes.domain.model.AppError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * O pouco de MCP que o app precisa: montar um `tools/call` (JSON-RPC 2.0) e ler a resposta.
 * O servidor responde em SSE ("event: message" + "data: {...}") e não exige sessão.
 */
internal object McpProtocol {
    fun toolCall(id: Int, name: String, arguments: JsonObject): JsonObject = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        put("method", "tools/call")
        putJsonObject("params") {
            put("name", name)
            put("arguments", arguments)
        }
    }

    /** Devolve o JSON que a tool produziu, ou lança o [AppError] correspondente. */
    fun toolPayload(body: String): String {
        val message = parseMessage(body)
        (message["error"] as? JsonObject)?.let { error ->
            throw AppError.Server(error["message"].stringOrNull())
        }
        val result = message["result"] as? JsonObject ?: throw AppError.Unexpected("Resposta MCP sem result")
        val text = (result["content"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonObject)?.get("text").stringOrNull() }
            .joinToString("")
        if ((result["isError"] as? JsonPrimitive)?.booleanOrNull == true) {
            throw if (text.contains("not found", ignoreCase = true)) AppError.NotFound else AppError.Server(text.take(160))
        }
        if (text.isBlank()) throw AppError.Unexpected("Tool sem conteúdo")
        return text
    }

    /** Aceita JSON puro ou SSE; no SSE vale a última linha `data:` com um objeto JSON. */
    fun parseMessage(body: String): JsonObject {
        val trimmed = body.trim()
        val json = if (trimmed.startsWith("{")) {
            trimmed
        } else {
            trimmed.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("data:") }
                .map { it.removePrefix("data:").trim() }
                .lastOrNull { it.startsWith("{") }
                ?: throw AppError.Unexpected("Resposta MCP vazia")
        }
        val element = try {
            Json.parseToJsonElement(json)
        } catch (e: IllegalArgumentException) {
            throw AppError.Unexpected("Resposta MCP inválida")
        }
        return element as? JsonObject ?: throw AppError.Unexpected("Resposta MCP inválida")
    }

    private fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull
}
