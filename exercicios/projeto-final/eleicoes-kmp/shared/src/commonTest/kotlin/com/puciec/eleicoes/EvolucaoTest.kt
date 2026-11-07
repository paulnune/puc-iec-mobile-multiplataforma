package com.puciec.eleicoes

import com.puciec.eleicoes.domain.evolucao.wealthVariation
import com.puciec.eleicoes.domain.model.Filing
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Variação de patrimônio entre eleições (#7): 2026 contra a eleição anterior mais recente. */
class EvolucaoTest {

    @Test
    fun `compara 2026 com a eleicao anterior mais recente`() {
        val timeline = listOf(
            filing(2018, totalAssets = 100.0),
            filing(2022, totalAssets = 150.0),
            filing(2026, totalAssets = 300.0),
        )

        val change = wealthVariation(timeline)

        assertEquals(2022, change?.previousYear)
        assertEquals(150.0, change?.previousAssets)
        assertEquals(300.0, change?.currentAssets)
        assertEquals(150.0, change?.delta)
        assertEquals(1.0, change?.ratio)
    }

    @Test
    fun `variacao negativa tem delta e razao negativos`() {
        val timeline = listOf(
            filing(2022, totalAssets = 200.0),
            filing(2026, totalAssets = 100.0),
        )

        val change = wealthVariation(timeline)

        assertEquals(-100.0, change?.delta)
        assertEquals(-0.5, change?.ratio)
    }

    @Test
    fun `sem eleicao anterior nao ha delta nem razao`() {
        val change = wealthVariation(listOf(filing(2026, totalAssets = 300.0)))

        assertNull(change?.previousYear)
        assertNull(change?.delta)
        assertNull(change?.ratio)
        assertEquals(300.0, change?.currentAssets)
    }

    @Test
    fun `razao fica nula quando o patrimonio anterior e zero`() {
        val timeline = listOf(
            filing(2022, totalAssets = 0.0),
            filing(2026, totalAssets = 500.0),
        )

        val change = wealthVariation(timeline)

        assertEquals(500.0, change?.delta)
        assertNull(change?.ratio)
    }

    @Test
    fun `sem candidatura em 2026 nao ha variacao`() {
        val timeline = listOf(filing(2018, totalAssets = 100.0), filing(2022, totalAssets = 150.0))

        assertNull(wealthVariation(timeline))
    }

    private fun filing(year: Int, totalAssets: Double) = Filing(
        candidateId = "cand-1",
        year = year,
        electionName = "Eleições $year",
        office = Office.PRESIDENT,
        officeLabel = "Presidente",
        state = "BR",
        partyId = "party-1",
        partyAcronym = "PT",
        number = 13,
        totalAssets = totalAssets,
        assetsDisclosed = true,
        outcome = Outcome.ELECTED,
    )
}
