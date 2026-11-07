package com.puciec.eleicoes.domain.evolucao

/** Os três chips do ranking de variação (#7). */
enum class RankingSort {
    /** Maior variação em módulo (ganhou ou perdeu). */
    BIGGEST_CHANGE,

    /** Só quem ganhou patrimônio, do maior ganho ao menor. */
    MOST_GAINED,

    /** Só quem perdeu patrimônio, da maior perda à menor. */
    MOST_LOST,
}

/**
 * Ordena as variações segundo o [sort]. Quem não tem eleição anterior (delta nulo) fica fora.
 */
fun rankWealthChanges(changes: List<WealthChange>, sort: RankingSort): List<WealthChange> {
    val withDelta = changes.filter { it.delta != null }
    return when (sort) {
        RankingSort.BIGGEST_CHANGE -> withDelta.sortedByDescending { kotlin.math.abs(it.delta!!) }
        RankingSort.MOST_GAINED -> withDelta.filter { it.delta!! > 0 }.sortedByDescending { it.delta }
        RankingSort.MOST_LOST -> withDelta.filter { it.delta!! < 0 }.sortedBy { it.delta }
    }
}
