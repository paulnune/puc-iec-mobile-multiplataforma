package com.puciec.eleicoes.android

import com.puciec.eleicoes.android.feature.evolucao.EvolucaoGroup
import com.puciec.eleicoes.android.feature.evolucao.EvolucaoViewModel
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
import com.puciec.eleicoes.data.repository.EvolucaoRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.evolucao.RankingSort
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** EvolucaoViewModel (#7) contra uma fonte falsa: nada sai para a rede. */
@OptIn(ExperimentalCoroutinesApi::class)
class EvolucaoViewModelTest {

    /** Fonte falsa: candidatos de 2026 e a linha do tempo de cada um; anota o último filtro. */
    private class FakeSource(
        private val candidates: List<CandidateDto> = emptyList(),
        private val filings: Map<String, List<FilingDto>> = emptyMap(),
        private val fail: AppError? = null,
    ) : EleicoesDataSource {
        var lastFilter: CandidateFilter? = null

        override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> {
            lastFilter = filter
            fail?.let { throw it }
            return PageDto(candidates, PaginationDto(page = page, limit = limit, total = candidates.size, totalPages = 1))
        }

        override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> = filings[candidateId].orEmpty()
        override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> =
            PageDto(emptyList(), PaginationDto(page = 1, totalPages = 1))

        override suspend fun getCandidate(id: String): CandidateDto = TODO()
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
        EvolucaoViewModel(EvolucaoRepository(CandidatesRepository(source, PartiesRepository(source))))

    private fun candidate(id: String, office: String = "president", state: String = "BR") = CandidateDto(
        id = id, partyId = "p1", ballotName = id.uppercase(), office = office,
        officeLabel = "PRESIDENTE", state = state, totalAssets = 300.0, assetsDisclosed = true,
    )

    private fun filing(id: String, year: Int, totalAssets: Double) = FilingDto(
        id = id, office = "president", officeLabel = "PRESIDENTE", state = "BR",
        totalAssets = totalAssets, assetsDisclosed = true, year = year,
    )

    /** a: +200 (ganhou). b: −500 (perdeu). c: sem eleição anterior. */
    private fun sourceWithThree() = FakeSource(
        candidates = listOf(candidate("a"), candidate("b"), candidate("c")),
        filings = mapOf(
            "a" to listOf(filing("a", 2022, 100.0), filing("a", 2026, 300.0)),
            "b" to listOf(filing("b", 2022, 800.0), filing("b", 2026, 300.0)),
            "c" to listOf(filing("c", 2026, 50.0)),
        ),
    )

    @Test
    fun `carrega a presidencia e ordena pela maior variacao`() {
        val vm = viewModel(sourceWithThree())

        assertEquals(listOf("b", "a"), vm.state.value.ranking.map { it.candidate.id })
    }

    @Test
    fun `quem nao tem eleicao anterior fica fora do ranking e aparece a parte`() {
        val vm = viewModel(sourceWithThree())

        assertEquals(listOf("c"), vm.state.value.semHistorico.map { it.candidate.id })
        assertTrue(vm.state.value.ranking.none { it.candidate.id == "c" })
    }

    @Test
    fun `chip mais perdeu mostra so quem perdeu patrimonio`() {
        val vm = viewModel(sourceWithThree())

        vm.onSortChange(RankingSort.MOST_LOST)

        assertEquals(listOf("b"), vm.state.value.ranking.map { it.candidate.id })
    }

    @Test
    fun `grupo governadores e senado usa a UF escolhida`() {
        val source = sourceWithThree()
        val vm = viewModel(source)

        vm.onGroupChange(EvolucaoGroup.GOVERNOR_SENATE)
        vm.onUfChange("RJ")

        assertEquals(setOf(Office.GOVERNOR, Office.SENATOR), source.lastFilter?.offices)
        assertEquals(setOf("RJ"), source.lastFilter?.states)
    }

    @Test
    fun `tocar abre a linha do tempo e fechar limpa a selecao`() {
        val vm = viewModel(sourceWithThree())

        vm.onSelect("a")
        assertEquals("a", vm.state.value.selected?.candidate?.id)

        vm.onSheetDismiss()
        assertNull(vm.state.value.selected)
    }

    @Test
    fun `falha da API vira estado de erro`() {
        val vm = viewModel(FakeSource(fail = AppError.NoConnection))

        assertEquals(UiState.Error(AppError.NoConnection), vm.state.value.items)
    }
}
