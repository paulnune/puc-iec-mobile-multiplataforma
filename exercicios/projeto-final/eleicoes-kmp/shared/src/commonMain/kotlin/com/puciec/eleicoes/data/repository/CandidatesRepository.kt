package com.puciec.eleicoes.data.repository

import com.puciec.eleicoes.data.mapper.toDomain
import com.puciec.eleicoes.data.mapper.toPage
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.Asset
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Filing
import com.puciec.eleicoes.domain.model.Page
import com.puciec.eleicoes.domain.model.WealthChange
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.time.Duration.Companion.minutes

/** Candidatos, bens e linha do tempo. Usado por Ranking (#2, #4), Detalhe (#3) e Evolução (#7). */
class CandidatesRepository(
    private val source: EleicoesDataSource,
    private val parties: PartiesRepository,
) {
    private val cache = MemoryCache(ttl = 5.minutes)

    /** Candidaturas de 2026 do maior para o menor patrimônio declarado, uma página por vez. */
    suspend fun ranking(
        filter: CandidateFilter = CandidateFilter(),
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Page<Candidate> = cache.getOrLoad("ranking|$filter|$page|$pageSize") {
        val acronyms = parties.acronymsById()
        source.listCandidates(filter, page, pageSize).toPage(page) { it.toDomain(acronyms) }
    }

    suspend fun candidate(id: String): Candidate = cache.getOrLoad("candidate|$id") {
        source.getCandidate(id).toDomain(parties.acronymsById())
    }

    /** Bens declarados ao TSE, na ordem da declaração. */
    suspend fun assets(candidateId: String, page: Int = 1, pageSize: Int = MAX_PAGE_SIZE): Page<Asset> =
        cache.getOrLoad("assets|$candidateId|$page|$pageSize") {
            source.listCandidateAssets(candidateId, page, pageSize).toPage(page) { it.toDomain() }
        }

    /** Todas as candidaturas da mesma pessoa desde 2006, da mais antiga para a mais recente. */
    suspend fun filings(candidateId: String): List<Filing> = cache.getOrLoad("filings|$candidateId") {
        val acronyms = parties.acronymsById()
        source.getCandidateFilings(candidateId).map { it.toDomain(acronyms) }.sortedBy { it.year }
    }

    /** A tool está quebrada no servidor desde 07/11; o #7 calcula a variação a partir de [filings]. */
    suspend fun wealthChanges(
        sort: WealthSort,
        filter: CandidateFilter = CandidateFilter(),
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Page<WealthChange> = cache.getOrLoad("wealth|$sort|$filter|$page|$pageSize") {
        val acronyms = parties.acronymsById()
        source.listWealthChanges(sort, filter, page, pageSize).toPage(page) { it.toDomain(acronyms) }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
        const val MAX_PAGE_SIZE = 50
    }
}
