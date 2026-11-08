package com.puciec.eleicoes.ui

import com.puciec.eleicoes.ui.feature.detalhe.DetalheViewModel
import com.puciec.eleicoes.ui.state.UiState
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
import com.puciec.eleicoes.domain.detalhe.AssetSummary
import com.puciec.eleicoes.domain.favorites.InMemoryFavoritesRepository
import com.puciec.eleicoes.domain.favorites.isFavorite
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** DetalheViewModel (#3) contra uma fonte falsa: nada sai para a rede. */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalheViewModelTest {

    /**
     * Fonte falsa: um candidato e os bens dele em páginas. [totalPages] permite simular uma API que anuncia
     * mais páginas do que o app deve buscar. As falhas podem ser desligadas no meio do teste (retry).
     */
    private class FakeSource(
        private val candidate: CandidateDto = CANDIDATE,
        private val assetPages: List<List<AssetDto>> = listOf(emptyList()),
        private val totalPages: Int = assetPages.size,
        private val total: Int = assetPages.sumOf { it.size },
        var failCandidate: AppError? = null,
        var failAssets: AppError? = null,
    ) : EleicoesDataSource {
        var assetCalls = 0

        override suspend fun getCandidate(id: String): CandidateDto {
            failCandidate?.let { throw it }
            return candidate
        }

        override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> {
            assetCalls++
            failAssets?.let { throw it }
            val items = assetPages.getOrNull(page - 1) ?: assetPages.last()
            return PageDto(items, PaginationDto(page = page, limit = limit, total = total, totalPages = totalPages))
        }

        override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> =
            PageDto(listOf(PartyDto(id = "p1", number = 45, acronym = "PSDB")), PaginationDto(page = 1, totalPages = 1))

        override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> = TODO()
        override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> = TODO()
        override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> = TODO()
        override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> = TODO()
        override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto = TODO()
        override suspend fun getParty(id: String): PartyDto = TODO()
    }

    private val favorites = InMemoryFavoritesRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(source: FakeSource) =
        DetalheViewModel(CANDIDATE.id, CandidatesRepository(source, PartiesRepository(source)), favorites)

    private fun assetsOf(vm: DetalheViewModel): AssetSummary = assertIs<UiState.Success<AssetSummary>>(vm.assets.value).data

    @Test
    fun `carrega o candidato com o partido`() {
        val state = viewModel(FakeSource()).candidate.value

        val candidate = assertIs<UiState.Success<*>>(state).data as Candidate
        assertEquals("DOMINGOS SÁVIO", candidate.ballotName)
        assertEquals("PSDB", candidate.partyAcronym)
    }

    @Test
    fun `erro no candidato vira estado de erro`() {
        val vm = viewModel(FakeSource(failCandidate = AppError.NotFound))

        assertEquals(UiState.Error(AppError.NotFound), vm.candidate.value)
    }

    @Test
    fun `bens numa pagina somam e agrupam`() {
        val vm = viewModel(FakeSource(assetPages = listOf(listOf(VEICULO, TERRENO_1, TERRENO_2))))

        val summary = assetsOf(vm)
        assertEquals(19_195.2, summary.total, 0.001)
        assertEquals(listOf("Veículo automotor terrestre", "Terreno"), summary.groups.map { it.typeLabel })
        assertFalse(summary.isTruncated)
    }

    @Test
    fun `bens em duas paginas sao juntados`() {
        val source = FakeSource(assetPages = listOf(listOf(VEICULO), listOf(TERRENO_1, TERRENO_2)))
        val vm = viewModel(source)

        assertEquals(listOf(1, 2, 3), assetsOf(vm).assets.map { it.order })
        assertEquals(2, source.assetCalls)
    }

    @Test
    fun `para no teto de paginas e avisa que a lista foi cortada`() {
        val source = FakeSource(assetPages = listOf(listOf(TERRENO_1)), totalPages = 20, total = 1_000)
        val vm = viewModel(source)

        val summary = assetsOf(vm)
        assertEquals(DetalheViewModel.MAX_ASSET_PAGES, source.assetCalls)
        assertTrue(summary.isTruncated)
        assertEquals(1_000, summary.declaredCount)
    }

    @Test
    fun `sem bens vira estado vazio`() {
        val vm = viewModel(FakeSource(assetPages = listOf(emptyList())))

        assertIs<UiState.Empty>(vm.assets.value)
    }

    @Test
    fun `erro nos bens nao derruba o cabecalho`() {
        val vm = viewModel(FakeSource(assetPages = listOf(listOf(VEICULO)), failAssets = AppError.NoConnection))

        assertEquals(UiState.Error(AppError.NoConnection), vm.assets.value)
        assertIs<UiState.Success<*>>(vm.candidate.value)
    }

    @Test
    fun `retry depois de falha carrega candidato e bens`() {
        val source = FakeSource(
            assetPages = listOf(listOf(VEICULO)),
            failCandidate = AppError.Server(null),
            failAssets = AppError.Server(null),
        )
        val vm = viewModel(source)
        assertIs<UiState.Error>(vm.candidate.value)
        assertIs<UiState.Error>(vm.assets.value)

        source.failCandidate = null
        source.failAssets = null
        vm.retry()

        assertIs<UiState.Success<*>>(vm.candidate.value)
        assertEquals(1, assetsOf(vm).assets.size)
    }

    @Test
    fun `estrela continua ligada aos favoritos`() = runTest {
        val vm = viewModel(FakeSource())

        vm.toggleFavorite()

        assertTrue(favorites.isFavorite(CANDIDATE.id).first())
    }

    private companion object {
        val CANDIDATE = CandidateDto(
            id = "46a37142-2034-4c84-b494-71fe997e3c87",
            partyId = "p1",
            ballotName = "DOMINGOS SÁVIO",
            fullName = "DOMINGOS SAVIO CAMPOS RESENDE",
            number = 222,
            office = "senator",
            officeLabel = "SENADOR",
            state = "MG",
            birthDate = "1957-03-25",
            totalAssets = 2_403_414.92,
            assetsDisclosed = true,
        )
        val VEICULO = AssetDto(
            id = "a1", order = 1,
            typeLabel = "Veículo automotor terrestre: caminhão, automóvel, moto, etc.",
            description = "TRATOR MASSY FERGUSSON ANO 1983", value = 14_695.2,
        )
        val TERRENO_1 = AssetDto(id = "a2", order = 2, typeLabel = "Terreno", description = "TERRENO", value = 2_000.0)
        val TERRENO_2 = AssetDto(id = "a3", order = 3, typeLabel = "Terreno", description = "TERRENO", value = 2_500.0)
    }
}
