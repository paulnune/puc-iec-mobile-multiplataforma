package com.puciec.eleicoes.domain.filter

import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Outcome

enum class Gender(val apiValue: String, val label: String) {
    FEMININO("FEMININO", "Mulheres"),
    MASCULINO("MASCULINO", "Homens"),
}

/**
 * Filtro da lista de candidatos. Cada campo vira um parâmetro real da tool `list_candidates`
 * (feature 4): `office`, `state`, `partyId`, `gender`, `outcome` e `q`.
 * Os chips da tela (Executivo, Senado, Mulheres...) só montam um [CandidateFilter] (#4).
 */
data class CandidateFilter(
    val offices: Set<Office> = emptySet(),
    val states: Set<String> = emptySet(),
    val partyIds: Set<String> = emptySet(),
    val gender: Gender? = null,
    val outcome: Outcome? = null,
    val query: String? = null,
)
