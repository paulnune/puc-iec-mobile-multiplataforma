package com.puciec.eleicoes

import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PaginationDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.ResultDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Outcome
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/** Fonte falsa: devolve um candidato e um partido, e conta as chamadas. */
private class FakeSource(private val partiesFail: Boolean = false) : EleicoesDataSource {
    var candidateCalls = 0

    override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> {
        candidateCalls++
        val candidate = CandidateDto(
            id = "c1",
            partyId = "p22",
            ballotName = "FULANO",
            office = "governor",
            officeLabel = "GOVERNADOR",
            state = "SP",
            totalAssets = 1_000.0,
            assetsDisclosed = true,
            result = ResultDto(round = 1, outcome = "elected", final = true),
        )
        return PageDto(listOf(candidate), PaginationDto(page = page, limit = limit, total = 1, totalPages = 1))
    }

    override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> {
        if (partiesFail) throw AppError.Server("falha simulada")
        return PageDto(listOf(PartyDto(id = "p22", number = 22, acronym = "PL", name = "PARTIDO LIBERAL")), PaginationDto(page = 1, totalPages = 1))
    }

    override suspend fun getCandidate(id: String): CandidateDto = error("não usado")
    override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> = error("não usado")
    override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> = error("não usado")
    override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> = error("não usado")
    override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> = error("não usado")
    override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto = error("não usado")
    override suspend fun getParty(id: String): PartyDto = error("não usado")
}

class CandidatesRepositoryTest {
    @Test
    fun `ranking traz a sigla do partido e o cargo do dominio`() = runTest {
        val source = FakeSource()
        val repository = CandidatesRepository(source, PartiesRepository(source))

        val candidate = repository.ranking().items.single()

        assertEquals("PL", candidate.partyAcronym)
        assertEquals(Office.GOVERNOR, candidate.office)
        assertEquals(Outcome.ELECTED, candidate.outcome)
    }

    @Test
    fun `a mesma consulta usa o cache`() = runTest {
        val source = FakeSource()
        val repository = CandidatesRepository(source, PartiesRepository(source))

        repository.ranking()
        repository.ranking()

        assertEquals(1, source.candidateCalls)
    }

    @Test
    fun `se os partidos falham o ranking aparece sem sigla`() = runTest {
        val source = FakeSource(partiesFail = true)
        val repository = CandidatesRepository(source, PartiesRepository(source))

        val candidate = repository.ranking().items.single()

        assertNull(candidate.partyAcronym)
        assertEquals("FULANO", candidate.ballotName)
    }
}
