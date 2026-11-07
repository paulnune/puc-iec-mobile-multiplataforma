package com.puciec.eleicoes

import com.puciec.eleicoes.data.source.mcp.McpEleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Office
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** O adapter MCP contra um servidor falso (MockEngine do Ktor): nada sai para a rede. */
class McpEleicoesDataSourceTest {
    private val candidatePage = """
        {"data":[{"id":"c1","partyId":"p13","ballotName":"LULA","fullName":"LUIZ INACIO LULA DA SILVA",
        "number":13,"office":"president","officeLabel":"PRESIDENTE","state":"BR","totalAssets":4800000.0,
        "assetsDisclosed":true,"result":{"round":1,"outcome":"runoff","final":true}}],
        "pagination":{"page":1,"limit":2,"total":14,"totalPages":7}}
    """.trimIndent()

    private val sseHeaders = headersOf(HttpHeaders.ContentType, "text/event-stream")

    @Test
    fun `envia a chave e a tool e le a pagina de candidatos`() = runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            respond(sseToolResponse(candidatePage), HttpStatusCode.OK, sseHeaders)
        }
        val source = McpEleicoesDataSource(HttpClient(engine), apiKey = "chave-de-teste")

        val page = source.listCandidates(CandidateFilter(offices = setOf(Office.PRESIDENT)), page = 1, limit = 2)

        val request = captured!!
        assertEquals("Bearer chave-de-teste", request.headers[HttpHeaders.Authorization])
        val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
        assertTrue("\"name\":\"list_candidates\"" in body)
        assertTrue("\"office\":[\"president\"]" in body)
        assertEquals(14, page.pagination?.total)
        assertEquals("LULA", page.data.single().ballotName)
        assertEquals("runoff", page.data.single().result?.outcome)
    }

    @Test
    fun `401 vira chave recusada`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val source = McpEleicoesDataSource(HttpClient(engine), apiKey = "chave-errada")
        assertFailsWith<AppError.InvalidApiKey> { source.getCandidate("c1") }
    }

    @Test
    fun `429 traz o tempo de espera`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "30")) }
        val source = McpEleicoesDataSource(HttpClient(engine), apiKey = "chave-de-teste")
        val error = assertFailsWith<AppError.RateLimited> { source.getParty("p13") }
        assertEquals(30, error.retryAfterSeconds)
    }

    @Test
    fun `sem chave nem chama a rede`() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("", HttpStatusCode.OK)
        }
        val source = McpEleicoesDataSource(HttpClient(engine), apiKey = "")
        assertFailsWith<AppError.MissingApiKey> { source.getParty("p13") }
        assertEquals(0, calls)
    }
}
