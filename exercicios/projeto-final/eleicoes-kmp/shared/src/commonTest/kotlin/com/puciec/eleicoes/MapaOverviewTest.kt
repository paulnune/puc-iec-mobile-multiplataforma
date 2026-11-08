package com.puciec.eleicoes

import com.puciec.eleicoes.domain.mapa.GOVERNOR_HINT
import com.puciec.eleicoes.domain.mapa.LegendItem
import com.puciec.eleicoes.domain.mapa.buildMapaOverview
import com.puciec.eleicoes.domain.mapa.candidateRows
import com.puciec.eleicoes.domain.model.Office
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O que a aba Mapa (#6) mostra, montado a partir dos resultados da API. */
class MapaOverviewTest {
    private val brasil = placeOf(
        "BR",
        leader = vote("FLAVIO BOLSONARO", "PL", 56_104_503, 47.03),
        runnerUp = vote("LULA", "PT", 53_879_538, 45.16),
    )

    private val presidentUfs = listOf(
        placeOf("RJ", leader = vote("FLAVIO BOLSONARO", "PL", 4_966_844), runnerUp = vote("LULA", "PT", 4_000_000)),
        placeOf("SP", leader = vote("FLAVIO BOLSONARO", "PL", 12_922_023), runnerUp = vote("LULA", "PT", 10_000_000)),
        placeOf("ZZ", leader = vote("LULA", "PT", 157_387), runnerUp = vote("FLAVIO BOLSONARO", "PL", 100_000)),
        placeOf("AC", leader = vote("FLAVIO BOLSONARO", "PL", 150_000), runnerUp = vote("LULA", "PT", 120_000)),
        placeOf("BA", leader = vote("LULA", "PT", 5_664_771), runnerUp = vote("FLAVIO BOLSONARO", "PL", 2_000_000)),
    )

    @Test
    fun `estados ordenados pelos votos do lider`() {
        val overview = buildMapaOverview(Office.PRESIDENT, brasil, presidentUfs)
        assertEquals(listOf("SP", "BA", "RJ", "AC"), overview.states.map { it.code })
    }

    @Test
    fun `exterior sai da lista e fica separado`() {
        // O exterior tem mais votos que o Acre, mas não entra na ordenação: fica na linha própria.
        val overview = buildMapaOverview(Office.PRESIDENT, brasil, presidentUfs)
        assertTrue(overview.states.none { it.code == "ZZ" })
        assertEquals("ZZ", overview.exterior?.code)
        assertEquals("LULA", overview.exterior?.leader?.ballotName)
    }

    @Test
    fun `governador usa a dica e nao tem exterior`() {
        val governor = listOf(
            placeOf("SP", office = Office.GOVERNOR, leader = vote("TARCÍSIO", "REPUBLICANOS", 14_491_874)),
            placeOf("ZZ", office = Office.GOVERNOR, leader = vote("ALGUÉM", "PT", 10)),
        )
        val overview = buildMapaOverview(Office.GOVERNOR, national = null, places = governor)
        assertEquals("Toque um estado. A cor mostra quem teve mais votos na disputa daquele estado.", overview.headline)
        assertEquals(GOVERNOR_HINT, overview.headline)
        assertNull(overview.exterior)
        assertEquals(listOf("SP"), overview.states.map { it.code })
    }

    @Test
    fun `presidente usa a frase nacional`() {
        val overview = buildMapaOverview(Office.PRESIDENT, brasil, presidentUfs)
        assertEquals(
            "No Brasil, Flavio Bolsonaro teve 56.104.503 votos e Lula 53.879.538. " +
                "Diferença de 2,2 milhões de votos. 100% das seções apuradas.",
            overview.headline,
        )
    }

    @Test
    fun `legenda tem um item por partido na ordem da lista`() {
        val presidente = buildMapaOverview(Office.PRESIDENT, brasil, presidentUfs)
        assertEquals(
            listOf(LegendItem("PL", "Flavio Bolsonaro"), LegendItem("PT", "Lula")),
            presidente.legend,
        )
        // Dois líderes do mesmo partido: a legenda leva o primeiro da lista (o de mais votos).
        val governador = buildMapaOverview(
            Office.GOVERNOR,
            national = null,
            places = listOf(
                placeOf("SC", office = Office.GOVERNOR, leader = vote("JORGINHO MELLO", "PL", 2_942_386)),
                placeOf("RJ", office = Office.GOVERNOR, leader = vote("DOUGLAS RUAS", "PL", 4_271_199)),
                placeOf("BA", office = Office.GOVERNOR, leader = vote("JERÔNIMO RODRIGUES", "PT", 4_626_509)),
            ),
        )
        assertEquals(
            listOf(LegendItem("PT", "Jerônimo Rodrigues"), LegendItem("PL", "Douglas Ruas")),
            governador.legend,
        )
    }

    @Test
    fun `uf sem lider fica fora da lista e da legenda`() {
        val places = presidentUfs + placeOf("AP", leader = null) + placeOf("PB", leader = vote("X", "NOVO", 0))
        val overview = buildMapaOverview(Office.PRESIDENT, brasil, places)
        assertEquals(listOf("AP"), overview.noData)
        assertTrue(overview.states.none { it.code == "AP" })
        // PB tem líder (mesmo com 0 votos) e entra na lista e na legenda; AP não.
        assertTrue(overview.states.any { it.code == "PB" })
        assertEquals(setOf("PL", "PT", "NOVO"), overview.legend.map { it.partyAcronym }.toSet())
    }

    @Test
    fun `votos da UF em ordem decrescente`() {
        val saoPaulo = placeOf(
            "SP",
            office = Office.GOVERNOR,
            votes = listOf(
                vote("VERA LÚCIA", "PSTU", 75_641),
                vote("TARCÍSIO", "REPUBLICANOS", 14_491_874),
                vote("CARLOS MACHADO", "PCB", 58_862),
                vote("FERNANDO HADDAD", "PT", 8_423_656),
                vote("VIVIAN MENDES", "UP", 80_480),
            ),
        )
        assertEquals(
            listOf("TARCÍSIO", "FERNANDO HADDAD", "VIVIAN MENDES", "VERA LÚCIA", "CARLOS MACHADO"),
            candidateRows(saoPaulo).map { it.ballotName },
        )
    }

    @Test
    fun `lista vazia gera visao sem estados`() {
        val overview = buildMapaOverview(Office.PRESIDENT, national = null, places = emptyList())
        assertTrue(overview.states.isEmpty())
        assertTrue(overview.legend.isEmpty())
        assertTrue(overview.noData.isEmpty())
        assertNull(overview.exterior)
        assertTrue(overview.isEmpty)
    }
}
