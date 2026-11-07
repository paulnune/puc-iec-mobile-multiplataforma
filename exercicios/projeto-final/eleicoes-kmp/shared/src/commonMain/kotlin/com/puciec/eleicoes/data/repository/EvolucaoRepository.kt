package com.puciec.eleicoes.data.repository

import com.puciec.eleicoes.domain.evolucao.CandidateEvolucao
import com.puciec.eleicoes.domain.evolucao.wealthVariation
import com.puciec.eleicoes.domain.filter.CandidateFilter

/**
 * Patrimônio entre eleições (#7). A tool `list_wealth_changes` está quebrada no servidor,
 * então o grupo sai de `ranking` e a variação é calculada aqui, a partir de `filings` de cada um.
 * Uma chamada de linha do tempo por candidato: cuidado com o limite de 60 req/min.
 */
class EvolucaoRepository(private val candidates: CandidatesRepository) {

    /** Candidatos do [filter] (até uma página) com a linha do tempo e a variação de cada um. */
    suspend fun variations(filter: CandidateFilter): List<CandidateEvolucao> =
        candidates.ranking(filter, pageSize = CandidatesRepository.MAX_PAGE_SIZE).items.map { candidate ->
            val timeline = candidates.filings(candidate.id)
            CandidateEvolucao(candidate, timeline, wealthVariation(timeline))
        }
}
