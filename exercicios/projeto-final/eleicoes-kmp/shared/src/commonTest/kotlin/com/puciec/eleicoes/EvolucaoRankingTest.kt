package com.puciec.eleicoes

import com.puciec.eleicoes.domain.evolucao.RankingSort
import com.puciec.eleicoes.domain.evolucao.WealthVariation
import com.puciec.eleicoes.domain.evolucao.rankWealthVariations
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ranking de variação de patrimônio (#7): os chips Maior variação, Mais ganhou e Mais perdeu. */
class EvolucaoRankingTest {

    private val ganhou = change("A", delta = 500.0)
    private val perdeu = change("B", delta = -800.0)
    private val pouco = change("C", delta = 100.0)
    private val semHistorico = change("D", delta = null)

    @Test
    fun `maior variacao ordena pelo modulo do delta do maior ao menor`() {
        val ranking = rankWealthVariations(listOf(pouco, ganhou, perdeu, semHistorico), RankingSort.BIGGEST_CHANGE)

        assertEquals(listOf("B", "A", "C"), ranking.map { it.candidateId })
    }

    @Test
    fun `mais ganhou mostra so deltas positivos do maior ao menor`() {
        val ranking = rankWealthVariations(listOf(pouco, ganhou, perdeu, semHistorico), RankingSort.MOST_GAINED)

        assertEquals(listOf("A", "C"), ranking.map { it.candidateId })
    }

    @Test
    fun `mais perdeu mostra so deltas negativos do mais negativo ao menos`() {
        val ranking = rankWealthVariations(listOf(pouco, ganhou, perdeu, semHistorico), RankingSort.MOST_LOST)

        assertEquals(listOf("B"), ranking.map { it.candidateId })
    }

    @Test
    fun `quem nao tem eleicao anterior fica fora do ranking`() {
        val ranking = rankWealthVariations(listOf(semHistorico), RankingSort.BIGGEST_CHANGE)

        assertEquals(emptyList(), ranking.map { it.candidateId })
    }

    private fun change(id: String, delta: Double?) = WealthVariation(
        candidateId = id,
        currentYear = 2026,
        currentAssets = 0.0,
        previousYear = delta?.let { 2022 },
        previousAssets = delta?.let { 0.0 },
        delta = delta,
        ratio = null,
    )
}
