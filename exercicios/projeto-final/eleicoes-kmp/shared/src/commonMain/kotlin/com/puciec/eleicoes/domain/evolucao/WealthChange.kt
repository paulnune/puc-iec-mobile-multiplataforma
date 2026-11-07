package com.puciec.eleicoes.domain.evolucao

import com.puciec.eleicoes.domain.model.Filing

/** Eleição de referência usada para calcular a variação de patrimônio (#7). */
const val CURRENT_YEAR = 2026

/**
 * Variação do patrimônio declarado de uma pessoa entre a eleição anterior e [CURRENT_YEAR].
 *
 * [delta] e [ratio] são nulos quando não há eleição anterior; [ratio] também é nulo quando
 * o patrimônio anterior é zero (divisão por zero).
 */
data class WealthChange(
    val candidateId: String,
    val currentYear: Int,
    val currentAssets: Double,
    val previousYear: Int?,
    val previousAssets: Double?,
    val delta: Double?,
    val ratio: Double?,
)

/**
 * Compara a candidatura de [CURRENT_YEAR] com a eleição anterior mais recente da [timeline].
 * Retorna nulo quando a pessoa não tem candidatura em [CURRENT_YEAR].
 */
fun wealthChange(timeline: List<Filing>): WealthChange? {
    val current = timeline.firstOrNull { it.year == CURRENT_YEAR } ?: return null
    val previous = timeline.filter { it.year < CURRENT_YEAR }.maxByOrNull { it.year }
    val delta = previous?.let { current.totalAssets - it.totalAssets }
    val ratio = if (previous != null && previous.totalAssets != 0.0) delta?.div(previous.totalAssets) else null

    return WealthChange(
        candidateId = current.candidateId,
        currentYear = CURRENT_YEAR,
        currentAssets = current.totalAssets,
        previousYear = previous?.year,
        previousAssets = previous?.totalAssets,
        delta = delta,
        ratio = ratio,
    )
}
