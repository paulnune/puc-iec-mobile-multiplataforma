package com.puciec.eleicoes.android

import com.puciec.eleicoes.android.feature.mapa.MapaViewModel
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.VoteDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.repository.ResultsRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.mapa.GOVERNOR_HINT
import com.puciec.eleicoes.domain.mapa.MapaOverview
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceResult
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** MapaViewModel (#6) contra uma fonte falsa: nada sai para a rede. */
@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModelTest {
    private class Call(val office: Office, val scope: PlaceScope, val code: String?, val round: Int?)

    /** Fonte falsa: responde Presidente e Governador e anota cada consulta. */
    private class FakeSource : EleicoesDataSource {
        val calls = mutableListOf<Call>()
        var failPlaces = false
        var failPlace = false
        var empty = false

        private fun vote(name: String, party: String, votes: Long, share: Double) =
            VoteDto(votes = votes, voteShare = share, ballotName = name, partyAcronym = party)

        private fun place(office: Office, scope: PlaceScope, code: String, leader: VoteDto, runner: VoteDto) =
            PlaceResultDto(
                office = office.apiValue, scope = scope.apiValue, state = code, code = code, round = 1,
                final = true, countedShare = 100.0, leader = leader, runnerUp = runner, votes = listOf(runner, leader),
            )

        private val flavio = { votes: Long -> vote("FLAVIO BOLSONARO", "PL", votes, 50.0) }
        private val lula = { votes: Long -> vote("LULA", "PT", votes, 45.0) }

        override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> {
            calls += Call(office, scope, null, round)
            if (failPlaces) throw AppError.NoConnection
            if (empty) return emptyList()
            return when {
                office == Office.PRESIDENT && scope == PlaceScope.BRASIL ->
                    listOf(place(office, scope, "BR", flavio(56_104_503), lula(53_879_538)))
                office == Office.PRESIDENT -> listOf(
                    place(office, scope, "BA", lula(5_664_771), flavio(2_000_000)),
                    place(office, scope, "SP", flavio(12_922_023), lula(10_000_000)),
                    place(office, scope, "ZZ", lula(157_387), flavio(100_000)),
                )
                else -> listOf(
                    place(office, scope, "SP", vote("TARCÍSIO", "REPUBLICANOS", 14_491_874, 62.7), vote("FERNANDO HADDAD", "PT", 8_423_656, 36.4)),
                    place(office, scope, "BA", vote("JERÔNIMO RODRIGUES", "PT", 4_626_509, 55.8), vote("ACM NETO", "UNIÃO", 3_625_227, 43.7)),
                )
            }
        }

        override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto {
            calls += Call(office, scope, code, round)
            if (failPlace) throw AppError.Server("HTTP 500")
            return place(office, scope, code, vote("TARCÍSIO", "REPUBLICANOS", 14_491_874, 62.7), vote("FERNANDO HADDAD", "PT", 8_423_656, 36.4))
        }

        override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> = TODO()
        override suspend fun getCandidate(id: String): CandidateDto = TODO()
        override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> = TODO()
        override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> = TODO()
        override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> = TODO()
        override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> = TODO()
        override suspend fun getParty(id: String): PartyDto = TODO()
    }

    private val source = FakeSource()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = MapaViewModel(ResultsRepository(source))

    private fun MapaViewModel.overview(): MapaOverview = assertIs<UiState.Success<MapaOverview>>(state.value.overview).data

    @Test
    fun `abre com presidente e carrega a visao geral`() {
        val vm = viewModel()
        assertEquals(Office.PRESIDENT, vm.state.value.office)
        val overview = vm.overview()
        assertEquals(
            "No Brasil, Flavio Bolsonaro teve 56.104.503 votos e Lula 53.879.538. " +
                "Diferença de 2,2 milhões de votos. 100% das seções apuradas.",
            overview.headline,
        )
        assertEquals(listOf("SP", "BA"), overview.states.map { it.code })
        assertEquals("ZZ", overview.exterior?.code)
        assertNull(vm.state.value.selectedCode)
    }

    @Test
    fun `todas as consultas pedem o primeiro turno`() {
        val vm = viewModel()
        vm.onOfficeChange(Office.GOVERNOR)
        vm.onPlaceSelected("SP")
        assertTrue(source.calls.size >= 4, "esperava consultas de Brasil, UFs, governador e UF")
        assertTrue(source.calls.all { it.round == 1 }, "rounds: ${source.calls.map { it.round }}")
    }

    @Test
    fun `trocar para governador recarrega sem consulta nacional`() {
        val vm = viewModel()
        source.calls.clear()
        vm.onOfficeChange(Office.GOVERNOR)
        assertEquals(Office.GOVERNOR, vm.state.value.office)
        val overview = vm.overview()
        assertEquals(GOVERNOR_HINT, overview.headline)
        assertEquals(listOf("SP", "BA"), overview.states.map { it.code })
        assertEquals("TARCÍSIO", overview.states.first().leader?.ballotName)
        assertNull(overview.exterior)
        assertTrue(source.calls.none { it.scope == PlaceScope.BRASIL })
        assertTrue(source.calls.all { it.office == Office.GOVERNOR })
    }

    @Test
    fun `falha vira erro e tentar de novo recarrega`() {
        source.failPlaces = true
        val vm = viewModel()
        val error = assertIs<UiState.Error>(vm.state.value.overview)
        assertEquals(AppError.NoConnection, error.error)
        source.failPlaces = false
        vm.retry()
        assertEquals(listOf("SP", "BA"), vm.overview().states.map { it.code })
    }

    @Test
    fun `lista vazia vira estado vazio`() {
        source.empty = true
        val vm = viewModel()
        assertIs<UiState.Empty>(vm.state.value.overview)
    }

    @Test
    fun `tocar numa UF carrega os votos no sheet`() {
        val vm = viewModel()
        vm.onPlaceSelected("SP")
        assertEquals("SP", vm.state.value.selectedCode)
        val place = assertIs<UiState.Success<PlaceResult>>(vm.state.value.place).data
        assertEquals("SP", place.code)
        assertEquals(2, place.votes.size)
        assertTrue(source.calls.any { it.code == "SP" && it.office == Office.PRESIDENT && it.scope == PlaceScope.UF })
    }

    @Test
    fun `falha na UF vira erro so no sheet e retry refaz`() {
        val vm = viewModel()
        source.failPlace = true
        vm.onPlaceSelected("BA")
        assertIs<UiState.Error>(vm.state.value.place)
        assertIs<UiState.Success<MapaOverview>>(vm.state.value.overview)
        val placeCallsBefore = source.calls.count { it.code == "BA" }
        val listCallsBefore = source.calls.count { it.code == null }
        source.failPlace = false
        vm.retryPlace()
        assertEquals("BA", assertIs<UiState.Success<PlaceResult>>(vm.state.value.place).data.code)
        assertEquals(placeCallsBefore + 1, source.calls.count { it.code == "BA" })
        assertEquals(listCallsBefore, source.calls.count { it.code == null }, "retry da UF não recarrega o cargo")
    }

    @Test
    fun `fechar o sheet mantem o cargo`() {
        val vm = viewModel()
        vm.onOfficeChange(Office.GOVERNOR)
        vm.onPlaceSelected("SP")
        vm.onSheetDismiss()
        assertNull(vm.state.value.selectedCode)
        assertEquals(Office.GOVERNOR, vm.state.value.office)
        assertEquals(GOVERNOR_HINT, vm.overview().headline)
    }
}
