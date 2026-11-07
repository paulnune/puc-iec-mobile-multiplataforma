package com.puciec.eleicoes

import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PaginationDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.repository.EvolucaoRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/** Fonte falsa: candidatos de 2026 e a linha do tempo de cada um. */
private class EvolucaoSource(
    private val candidates: List<CandidateDto>,
    private val filings: Map<String, List<FilingDto>>,
) : EleicoesDataSource {
    var lastFilter: CandidateFilter? = null

    override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> {
        lastFilter = filter
        return PageDto(candidates, PaginationDto(page = page, limit = limit, total = candidates.size, totalPages = 1))
    }

    override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> = filings[candidateId].orEmpty()

    override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> =
        PageDto(emptyList(), PaginationDto(page = 1, totalPages = 1))

    override suspend fun getCandidate(id: String): CandidateDto = error("não usado")
    override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> = error("não usado")
    override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> = error("não usado")
    override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> = error("não usado")
    override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto = error("não usado")
    override suspend fun getParty(id: String): PartyDto = error("não usado")
}

private fun candidate(id: String) = CandidateDto(
    id = id,
    partyId = "p1",
    ballotName = id.uppercase(),
    office = "president",
    officeLabel = "PRESIDENTE",
    state = "BR",
    totalAssets = 300.0,
    assetsDisclosed = true,
)

private fun filing(id: String, year: Int, totalAssets: Double) = FilingDto(
    id = id,
    office = "president",
    officeLabel = "PRESIDENTE",
    state = "BR",
    totalAssets = totalAssets,
    assetsDisclosed = true,
    year = year,
)

class EvolucaoRepositoryTest {
    @Test
    fun `monta a variacao de cada candidato a partir da sua linha do tempo`() = runTest {
        val source = EvolucaoSource(
            candidates = listOf(candidate("a"), candidate("b")),
            filings = mapOf(
                "a" to listOf(filing("a", 2022, 100.0), filing("a", 2026, 300.0)),
                "b" to listOf(filing("b", 2026, 50.0)),
            ),
        )
        val repository = EvolucaoRepository(CandidatesRepository(source, PartiesRepository(source)))

        val items = repository.variations(CandidateFilter(offices = setOf(Office.PRESIDENT)))

        assertEquals(listOf("a", "b"), items.map { it.candidate.id })
        assertEquals(200.0, items[0].variation?.delta)
        assertEquals(2.0, items[0].variation?.ratio)
        assertEquals(2, items[0].timeline.size)
    }

    @Test
    fun `candidato sem eleicao anterior fica com variacao sem delta`() = runTest {
        val source = EvolucaoSource(
            candidates = listOf(candidate("novo")),
            filings = mapOf("novo" to listOf(filing("novo", 2026, 50.0))),
        )
        val repository = EvolucaoRepository(CandidatesRepository(source, PartiesRepository(source)))

        val item = repository.variations(CandidateFilter()).single()

        assertNull(item.variation?.delta)
        assertNull(item.variation?.previousYear)
    }

    @Test
    fun `repassa o filtro do grupo para a busca de candidatos`() = runTest {
        val source = EvolucaoSource(candidates = emptyList(), filings = emptyMap())
        val repository = EvolucaoRepository(CandidatesRepository(source, PartiesRepository(source)))
        val group = CandidateFilter(offices = setOf(Office.GOVERNOR, Office.SENATOR), states = setOf("SP"))

        repository.variations(group)

        assertEquals(group, source.lastFilter)
    }
}
