package com.puciec.eleicoes

import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.source.mcp.McpEleicoesDataSource
import com.puciec.eleicoes.domain.partidos.PartySort
import com.puciec.eleicoes.domain.partidos.displayName
import com.puciec.eleicoes.domain.partidos.filterAndSort
import com.puciec.eleicoes.domain.partidos.officialParties
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Lista da aba Partidos (#8) de ponta a ponta: resposta JSON da tool `list_parties` → adapter MCP (MockEngine,
 * nada sai para a rede) → PartiesRepository → officialParties → ordem por sigla.
 * Garante a tabela oficial do grupo com os dados reais e a resiliência a partidos novos no futuro.
 */
class PartidosApiTest {

    private val sseHeaders = headersOf(HttpHeaders.ContentType, "text/event-stream")

    /** Linhas "SIGLA | Nome" como a aba mostra com "Ordenar por Sigla", a partir de uma resposta da API. */
    private suspend fun rowsFromApi(json: String): List<String> {
        val engine = MockEngine { request ->
            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            assertTrue("\"name\":\"list_parties\"" in body, "o app deve chamar a tool list_parties")
            respond(sseToolResponse(json), HttpStatusCode.OK, sseHeaders)
        }
        val repository = PartiesRepository(McpEleicoesDataSource(HttpClient(engine), apiKey = "chave-de-teste"))
        return repository.all()
            .officialParties()
            .filterAndSort("", PartySort.ACRONYM)
            .map { "${it.acronym} | ${it.displayName()}" }
    }

    @Test
    fun `resposta real da API de hoje vira exatamente a tabela oficial`() = runTest {
        assertEquals(TABELA_OFICIAL_2026, rowsFromApi(listPartiesJson()))
    }

    @Test
    fun `partido novo devolvido pela API aparece como veio`() = runTest {
        val rows = rowsFromApi(
            listPartiesJson("""{"id": "novo-99", "number": 99, "acronym": "PNF", "name": "PARTIDO NOVO FUTURO"}"""),
        )

        assertTrue("PNF | Partido Novo Futuro" in rows)
        assertEquals(TABELA_OFICIAL_2026.size + 1, rows.size)
        // A tabela oficial continua inteira e na mesma ordem; o novo só entra na posição alfabética dele.
        assertEquals(TABELA_OFICIAL_2026, rows - "PNF | Partido Novo Futuro")
    }

    @Test
    fun `partido novo que reaproveita o numero de um extinto aparece`() = runTest {
        // O PSL (17) continua oculto, mas um partido novo com o 17 e outra sigla é mostrado.
        val rows = rowsFromApi(
            listPartiesJson("""{"id": "novo-17", "number": 17, "acronym": "MAIS", "name": "PARTIDO MAIS BRASIL"}"""),
        )

        assertTrue("MAIS | Partido Mais Brasil" in rows)
        assertTrue(rows.none { it.startsWith("PSL ") })
        assertEquals(TABELA_OFICIAL_2026, rows - "MAIS | Partido Mais Brasil")
    }

    @Test
    fun `partido novo com sigla de acento e nome com espaco duplo segue a formatacao normal`() = runTest {
        val rows = rowsFromApi(
            listPartiesJson("""{"id": "novo-88", "number": 88, "acronym": "AÇÃO", "name": "PARTIDO  AÇÃO  POPULAR"}"""),
        )

        assertTrue("AÇÃO | Partido Ação Popular" in rows)
    }
}
