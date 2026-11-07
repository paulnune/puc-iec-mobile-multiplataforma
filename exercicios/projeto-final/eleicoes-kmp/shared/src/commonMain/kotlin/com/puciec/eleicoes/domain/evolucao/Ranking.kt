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
fun rankWealthVariations(changes: List<WealthVariation>, sort: RankingSort): List<WealthVariation> {
    val withDelta = changes.filter { it.delta != null }
    return when (sort) {
        RankingSort.BIGGEST_CHANGE -> withDelta.sortedByDescending { kotlin.math.abs(it.delta!!) }
        RankingSort.MOST_GAINED -> withDelta.filter { it.delta!! > 0 }.sortedByDescending { it.delta }
        RankingSort.MOST_LOST -> withDelta.filter { it.delta!! < 0 }.sortedBy { it.delta }
    }
}

/** Ranking da tela: as variações ordenadas pelo chip, de volta aos candidatos (sem histórico fica fora). */
fun rankEvolucao(items: List<CandidateEvolucao>, sort: RankingSort): List<CandidateEvolucao> {
    val byId = items.associateBy { it.candidate.id }
    return rankWealthVariations(items.mapNotNull { it.variation }, sort).mapNotNull { byId[it.candidateId] }
}
