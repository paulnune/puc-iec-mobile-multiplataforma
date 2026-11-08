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
) {
    /** Troca a UF do seletor; `null` ou vazio significa "Todas" e limpa o parâmetro `state`. */
    fun withState(state: String?): CandidateFilter =
        copy(states = state?.trim()?.takeIf { it.isNotEmpty() }?.let { setOf(it.uppercase()) } ?: emptySet())
}

/**
 * Chips do topo do ranking (#4), na mesma ordem e com os mesmos rótulos da referência
 * eleicoes.dev/patrimonio. Cada chip é uma seleção única que troca os parâmetros `office`
 * e/ou `gender` da consulta; a UF e os demais campos do filtro atual são preservados.
 */
enum class RankingCategory(
    val label: String,
    val offices: Set<Office>,
    val gender: Gender? = null,
) {
    ALL("Todos", emptySet()),
    EXECUTIVE(
        "Executivo",
        setOf(Office.PRESIDENT, Office.GOVERNOR, Office.VICE_PRESIDENT, Office.VICE_GOVERNOR),
    ),
    LEGISLATIVE(
        "Legislativo",
        setOf(
            Office.SENATOR,
            Office.FEDERAL_DEPUTY,
            Office.STATE_DEPUTY,
            Office.DISTRICT_DEPUTY,
            Office.SENATOR_FIRST_ALTERNATE,
            Office.SENATOR_SECOND_ALTERNATE,
        ),
    ),
    PRESIDENCY("Presidência", setOf(Office.PRESIDENT)),
    GOVERNORS("Governadores", setOf(Office.GOVERNOR)),
    SENATE("Senado", setOf(Office.SENATOR, Office.SENATOR_FIRST_ALTERNATE, Office.SENATOR_SECOND_ALTERNATE)),
    CHAMBER("Câmara", setOf(Office.FEDERAL_DEPUTY)),
    ASSEMBLIES("Assembleias", setOf(Office.STATE_DEPUTY, Office.DISTRICT_DEPUTY)),
    WOMEN("Mulheres", emptySet(), Gender.FEMININO),
    ;

    /**
     * Aplica o chip sobre um filtro existente: `office`/`gender` vêm do chip, enquanto
     * `states`, `partyIds`, `outcome` e `query` continuam valendo.
     */
    fun toFilter(base: CandidateFilter = CandidateFilter()): CandidateFilter =
        base.copy(offices = offices, gender = gender)

    companion object {
        /** Chip que representa o filtro atual, para marcar visualmente a seleção. */
        fun of(filter: CandidateFilter): RankingCategory =
            if (filter.gender == Gender.FEMININO) {
                WOMEN
            } else {
                entries.firstOrNull { it != WOMEN && it.offices == filter.offices } ?: ALL
            }
    }
}

/** Atalho dos testes e de quem monta a consulta: chip + UF opcional viram um [CandidateFilter]. */
fun rankingFilter(category: RankingCategory, state: String? = null): CandidateFilter =
    category.toFilter().withState(state)
