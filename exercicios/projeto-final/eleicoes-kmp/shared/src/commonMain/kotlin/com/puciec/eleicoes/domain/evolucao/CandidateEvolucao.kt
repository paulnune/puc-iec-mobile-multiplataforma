package com.puciec.eleicoes.domain.evolucao

import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Filing

/** Um candidato de 2026 com a linha do tempo dele e a variação (nula quando não há 2026). */
data class CandidateEvolucao(
    val candidate: Candidate,
    val timeline: List<Filing>,
    val variation: WealthVariation?,
)
