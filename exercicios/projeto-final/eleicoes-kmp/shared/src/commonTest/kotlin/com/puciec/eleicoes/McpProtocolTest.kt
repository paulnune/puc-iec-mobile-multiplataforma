package com.puciec.eleicoes

import com.puciec.eleicoes.data.source.mcp.McpArguments
import com.puciec.eleicoes.data.source.mcp.McpProtocol
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.filter.Gender
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Office
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Resposta de tool no formato que o servidor manda: SSE com uma linha `data:`. */
internal fun sseToolResponse(text: String, isError: Boolean = false): String {
    val message = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", 1)
        putJsonObject("result") {
            putJsonArray("content") {
                addJsonObject {
                    put("type", "text")
                    put("text", text)
                }
            }
            if (isError) put("isError", true)
        }
    }
    return "event: message\ndata: $message\n\n"
}

class McpProtocolTest {
    @Test
    fun `le o texto da tool numa resposta SSE`() {
        assertEquals("""{"id":"c1"}""", McpProtocol.toolPayload(sseToolResponse("""{"id":"c1"}""")))
    }

    @Test
    fun `aceita JSON puro sem SSE`() {
        val body = """{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"[]"}]}}"""
        assertEquals("[]", McpProtocol.toolPayload(body))
    }

    @Test
    fun `tool que nao acha o item vira NotFound`() {
        assertFailsWith<AppError.NotFound> {
            McpProtocol.toolPayload(sseToolResponse("Candidate not found", isError = true))
        }
    }

    @Test
    fun `falha de SQL no servidor vira Server com o detalhe`() {
        val error = assertFailsWith<AppError.Server> {
            McpProtocol.toolPayload(sseToolResponse("Failed query: SELECT COUNT(*)", isError = true))
        }
        assertTrue(error.detail.orEmpty().startsWith("Failed query"))
    }

    @Test
    fun `erro JSON-RPC vira Server`() {
        val body = """{"jsonrpc":"2.0","id":1,"error":{"code":-32602,"message":"Invalid params"}}"""
        val error = assertFailsWith<AppError.Server> { McpProtocol.toolPayload(body) }
        assertEquals("Invalid params", error.detail)
    }

    @Test
    fun `resposta sem dados vira Unexpected`() {
        assertFailsWith<AppError.Unexpected> { McpProtocol.toolPayload("event: message\n\n") }
    }

    @Test
    fun `monta o tools call com o nome e os argumentos`() {
        val call = McpProtocol.toolCall(7, "get_candidate", McpArguments.id("c1"))
        assertEquals("tools/call", call["method"]!!.jsonPrimitive.content)
        val params = call["params"]!!.jsonObject
        assertEquals("get_candidate", params["name"]!!.jsonPrimitive.content)
        assertEquals("c1", params["arguments"]!!.jsonObject["id"]!!.jsonPrimitive.content)
    }
}

class McpArgumentsTest {
    @Test
    fun `filtro vira as listas que a tool espera`() {
        val filter = CandidateFilter(
            offices = setOf(Office.PRESIDENT, Office.GOVERNOR),
            states = setOf("SP"),
            gender = Gender.FEMININO,
            query = "  tabata ",
        )
        val args = McpArguments.candidates(filter, page = 2, limit = 30)

        assertEquals(2, args["page"]!!.jsonPrimitive.int)
        assertEquals(30, args["limit"]!!.jsonPrimitive.int)
        assertEquals(listOf("governor", "president"), args["office"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("SP"), args["state"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("FEMININO"), args["gender"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("tabata", args["q"]!!.jsonPrimitive.content)
    }

    @Test
    fun `filtro vazio manda so a paginacao`() {
        val args = McpArguments.candidates(CandidateFilter(), page = 1, limit = 20)
        assertEquals(setOf("page", "limit"), args.keys)
    }
}
