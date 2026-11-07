package com.puciec.eleicoes.android

import com.puciec.eleicoes.android.feature.ranking.RankingViewModel
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PaginationDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.filter.RankingCategory
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** Busca do ranking (#3/#4): local primeiro, com `q` no servidor quando não acha na lista. */
@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {

    /**
     * Fonte falsa no espírito da API: sem `q` devolve a lista carregada; com `q` devolve o
     * resultado da busca no servidor. Anota o último filtro para conferir os parâmetros.
     */
    private class FakeSource(
        private val all: List<CandidateDto>,
        private val searchResults: Map<String, List<CandidateDto>> = emptyMap(),
    ) : EleicoesDataSource {
        var lastFilter: CandidateFilter? = null
        var calls = 0

        override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> {
            lastFilter = filter
            calls++
            val items = filter.query?.let { query ->
                searchResults[query] ?: all.filter {
                    it.ballotName.contains(query, ignoreCase = true) ||
                        it.fullName.contains(query, ignoreCase = true)
                }
            } ?: all
            return PageDto(items, PaginationDto(page = page, limit = limit, total = items.size, totalPages = 1))
        }

        override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> =
            PageDto(emptyList(), PaginationDto(page = 1, totalPages = 1))

        override suspend fun getCandidate(id: String): CandidateDto = TODO()
        override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> = TODO()
        override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> = TODO()
        override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> = TODO()
        override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> = TODO()
        override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto = TODO()
        override suspend fun getParty(id: String): PartyDto = TODO()
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(source: FakeSource) =
        RankingViewModel(CandidatesRepository(source, PartiesRepository(source)))

    private fun candidate(id: String, ballotName: String = id, fullName: String = ballotName) = CandidateDto(
        id = id,
        partyId = "p1",
        ballotName = ballotName,
        fullName = fullName,
        office = "governor",
        officeLabel = "GOVERNADOR",
        state = "RN",
        gender = "MASCULINO",
        totalAssets = 100.0,
        assetsDisclosed = true,
    )

    private fun caduSource() = FakeSource(
        all = listOf(candidate("ana", "ANA MARIA")),
        searchResults = mapOf(
            "Cadu" to listOf(candidate("cadu", "CADU DE LULA", "CARLOS EDUARDO XAVIER")),
            "Natalia" to listOf(candidate("natalia", "NATÁLIA BONAVIDES", "NATALIA BASTOS BONAVIDES")),
        ),
    )

    private fun RankingViewModel.ids(): List<String> =
        (state.value.candidates as UiState.Success).data.map { it.id }

    @Test
    fun `texto que existe na lista nao consulta o servidor`() {
        val source = FakeSource(all = listOf(candidate("ana", "ANA MARIA")))
        val vm = viewModel(source)
        val before = source.calls

        vm.onSearchTextChange("ana")

        assertEquals(before, source.calls)
        assertNull(vm.state.value.filter.query)
    }

    @Test
    fun `busca sem resultado local vira q no servidor`() {
        val source = caduSource()
        val vm = viewModel(source)

        vm.onSearchTextChange("Cadu")

        assertEquals("Cadu", source.lastFilter?.query)
        assertEquals(listOf("cadu"), vm.ids())
        assertEquals("Cadu", vm.state.value.filter.query)
    }

    @Test
    fun `limpar a busca remove o q e recarrega a lista do filtro`() {
        val source = caduSource()
        val vm = viewModel(source)
        vm.onSearchTextChange("Cadu")

        vm.onSearchTextChange("")

        assertNull(vm.state.value.filter.query)
        assertEquals(listOf("ana"), vm.ids())
    }

    @Test
    fun `busca com acento acha no aparelho sem chamar a API`() {
        val source = FakeSource(all = listOf(candidate("natalia", "NATÁLIA BONAVIDES", "NATALIA BASTOS BONAVIDES")))
        val vm = viewModel(source)
        val before = source.calls

        vm.onSearchTextChange("natalia")

        assertEquals(before, source.calls)
        assertNull(vm.state.value.filter.query)
    }

    @Test
    fun `com busca no servidor ativa o q acompanha o novo texto`() {
        val source = caduSource()
        val vm = viewModel(source)
        vm.onSearchTextChange("Cadu")

        vm.onSearchTextChange("Natalia")

        assertEquals("Natalia", source.lastFilter?.query)
        assertEquals("Natalia", vm.state.value.filter.query)
        assertEquals(listOf("natalia"), vm.ids())
    }

    @Test
    fun `trocar o chip preserva o q da busca`() {
        val source = caduSource()
        val vm = viewModel(source)
        vm.onSearchTextChange("Cadu")

        vm.onFilterChange(RankingCategory.EXECUTIVE.toFilter(vm.state.value.filter))

        assertEquals("Cadu", source.lastFilter?.query)
        assertEquals(RankingCategory.EXECUTIVE.offices, source.lastFilter?.offices)
        assertEquals("Cadu", vm.state.value.filter.query)
    }
}
