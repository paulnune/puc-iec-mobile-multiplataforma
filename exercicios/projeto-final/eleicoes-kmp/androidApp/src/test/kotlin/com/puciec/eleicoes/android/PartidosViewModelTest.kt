package com.puciec.eleicoes.android

import com.puciec.eleicoes.android.feature.partidos.PartidosViewModel
import com.puciec.eleicoes.android.feature.partidos.PartyCount
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.data.cache.CacheStore
import com.puciec.eleicoes.data.cache.CachedResponse
import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PaginationDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.data.partidos.PartidosCatalog
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.filter.RankingCategory
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Party
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.WealthSort
import com.puciec.eleicoes.domain.partidos.PartySort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** PartidosViewModel (#8) contra uma fonte falsa e um "disco" em memória: nada sai para a rede. */
@OptIn(ExperimentalCoroutinesApi::class)
class PartidosViewModelTest {

    /**
     * Fonte falsa: partidos fixos e candidatos por partido em páginas. Conta as chamadas por partido e guarda o
     * último filtro, para conferir os parâmetros enviados à API.
     */
    private class FakeSource(
        var parties: List<PartyDto> = PARTIES,
        private val candidatesByParty: Map<String, List<List<CandidateDto>>> = emptyMap(),
        var failParties: AppError? = null,
        var failCandidates: AppError? = null,
    ) : EleicoesDataSource {
        var partyCalls = 0
        val candidateCalls = mutableMapOf<String, Int>()
        var lastFilter: CandidateFilter? = null
        val totalCandidateCalls get() = candidateCalls.values.sum()

        override suspend fun listParties(query: String?, page: Int, limit: Int): PageDto<PartyDto> {
            partyCalls++
            failParties?.let { throw it }
            return PageDto(parties, PaginationDto(page = 1, limit = limit, total = parties.size, totalPages = 1))
        }

        override suspend fun listCandidates(filter: CandidateFilter, page: Int, limit: Int): PageDto<CandidateDto> {
            val id = filter.partyIds.firstOrNull().orEmpty()
            candidateCalls[id] = (candidateCalls[id] ?: 0) + 1
            lastFilter = filter
            failCandidates?.let { throw it }
            val pages = candidatesByParty[id].orEmpty()
            val items = pages.getOrNull(page - 1).orEmpty()
            return PageDto(
                items,
                PaginationDto(page = page, limit = limit, total = pages.sumOf { it.size }, totalPages = pages.size.coerceAtLeast(1)),
            )
        }

        override suspend fun getParty(id: String): PartyDto = parties.first { it.id == id }
        override suspend fun getCandidate(id: String): CandidateDto = TODO()
        override suspend fun listCandidateAssets(candidateId: String, page: Int, limit: Int): PageDto<AssetDto> = TODO()
        override suspend fun getCandidateFilings(candidateId: String): List<FilingDto> = TODO()
        override suspend fun listWealthChanges(sort: WealthSort, filter: CandidateFilter, page: Int, limit: Int): PageDto<WealthChangeDto> = TODO()
        override suspend fun listPlaceResults(office: Office, scope: PlaceScope, state: String?, round: Int?): List<PlaceResultDto> = TODO()
        override suspend fun getPlaceResult(code: String, office: Office, scope: PlaceScope, round: Int?): PlaceResultDto = TODO()
    }

    /** "Disco" do aparelho em memória; o mesmo objeto entre dois ViewModels simula fechar e reabrir o app. */
    private class MemoryStore : CacheStore {
        val files = mutableMapOf<String, CachedResponse>()
        override suspend fun read(key: String) = files[key]
        override suspend fun write(key: String, response: CachedResponse) {
            files[key] = response
        }
    }

    private var clock = 1_000_000L
    private val disk = MemoryStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(source: FakeSource, store: CacheStore = disk): PartidosViewModel =
        PartidosViewModel(
            PartidosCatalog(source, store, now = { clock }),
            CandidatesRepository(source, PartiesRepository(source)),
        )

    private fun PartidosViewModel.partyList(): List<Party> =
        assertIs<UiState.Success<List<Party>>>(state.value.parties).data

    private fun PartidosViewModel.candidateList(): List<Candidate> =
        assertIs<UiState.Success<List<Candidate>>>(state.value.candidates).data

    private fun PartidosViewModel.party(acronym: String): Party = partyList().first { it.acronym == acronym }

    // --- Lista, busca e ordem ---

    @Test
    fun `carrega so os partidos de 2026 pelo numero com busca vazia`() {
        val vm = viewModel(FakeSource())

        // O PSL (17) vem da API mas não tem candidaturas em 2026: fica fora da lista oficial.
        assertEquals(listOf(10, 12, 45, 55), vm.partyList().map { it.number })
        assertEquals(PartySort.NUMBER, vm.state.value.sort)
        assertEquals("", vm.state.value.query)
        assertNull(vm.state.value.selected)
    }

    @Test
    fun `busca e ordem sao locais e nao chamam a API de novo`() {
        val source = FakeSource()
        val vm = viewModel(source)
        val calls = source.partyCalls

        vm.onQueryChange("social")
        vm.onSortChange(PartySort.ACRONYM)

        assertEquals(listOf("PSD", "PSDB"), vm.partyList().map { it.acronym })
        assertEquals(calls, source.partyCalls)
    }

    @Test
    fun `busca sem resultado vira estado vazio`() {
        val vm = viewModel(FakeSource())

        vm.onQueryChange("xyz")

        assertIs<UiState.Empty>(vm.state.value.parties)
    }

    @Test
    fun `sigla oficial substitui a grafia da API`() {
        val vm = viewModel(FakeSource(parties = PARTIES + PartyDto(id = "sd", number = 77, acronym = "SD", name = "SOLIDARIEDADE")))

        assertEquals("SOLIDARIEDADE", vm.partyList().first { it.number == 77 }.acronym)
    }

    @Test
    fun `erro na lista de partidos e retry`() {
        val source = FakeSource(failParties = AppError.NoConnection)
        val vm = viewModel(source)
        assertEquals(UiState.Error(AppError.NoConnection), vm.state.value.parties)

        source.failParties = null
        vm.retry()

        assertEquals(4, vm.partyList().size)
    }

    // --- Pré-carga (revisão 8) ---

    @Test
    fun `ao abrir a aba ja carrega a contagem de todos os partidos`() {
        val source = FakeSource(candidatesByParty = mapOf("psdb" to listOf(listOf(candidate("c1", "psdb"), candidate("c2", "psdb")))))
        val vm = viewModel(source)

        assertEquals(PartyCount.Loaded(2), vm.state.value.counts["psdb"])
        assertEquals(PartyCount.Loaded(0), vm.state.value.counts["rep"])
        assertEquals(setOf("psd", "rep", "psdb", "pdt"), vm.state.value.counts.keys)
        // Uma chamada por partido de 2026; o PSL (oculto) não é pré-carregado.
        assertEquals(4, source.totalCandidateCalls)
        assertNull(source.candidateCalls["psl"])
    }

    @Test
    fun `erro na contagem de um partido vira falha`() {
        val vm = viewModel(FakeSource(failCandidates = AppError.Server(null)))

        assertEquals(PartyCount.Failed, vm.state.value.counts["psdb"])
    }

    @Test
    fun `abrir o partido usa a pagina pre-carregada sem chamar a API`() {
        val source = FakeSource(candidatesByParty = mapOf("psdb" to listOf(listOf(candidate("c1", "psdb")))))
        val vm = viewModel(source)
        val calls = source.totalCandidateCalls

        vm.openParty(vm.party("PSDB"))

        assertEquals("PSDB", vm.state.value.selected?.acronym)
        assertEquals(listOf("c1"), vm.candidateList().map { it.id })
        assertEquals(1, vm.state.value.total)
        assertEquals(calls, source.totalCandidateCalls)
    }

    // --- Cache no aparelho (1 dia) e recarregar ---

    @Test
    fun `reabrir o app sem rede mostra tudo do aparelho`() {
        val online = FakeSource(candidatesByParty = mapOf("psdb" to listOf(listOf(candidate("c1", "psdb")))))
        viewModel(online)

        val offline = FakeSource(failParties = AppError.NoConnection, failCandidates = AppError.NoConnection)
        clock += 2.days.inWholeMilliseconds // vencido: tenta a API, falha e usa o salvo
        val vm = viewModel(offline)

        assertEquals(4, vm.partyList().size)
        assertEquals(PartyCount.Loaded(1), vm.state.value.counts["psdb"])
        assertTrue(vm.state.value.fromCache)
        vm.openParty(vm.party("PSDB"))
        assertEquals(listOf("c1"), vm.candidateList().map { it.id })
    }

    @Test
    fun `dentro de 1 dia reabrir o app nao chama a API`() {
        viewModel(FakeSource())
        val again = FakeSource()

        val vm = viewModel(again)

        assertEquals(4, vm.partyList().size)
        assertEquals(0, again.partyCalls)
        assertEquals(0, again.totalCandidateCalls)
        assertFalse(vm.state.value.fromCache)
    }

    @Test
    fun `abrir partido vencido ha mais de 1 dia busca de novo`() {
        val source = FakeSource(candidatesByParty = mapOf("psdb" to listOf(listOf(candidate("c1", "psdb")))))
        val vm = viewModel(source)
        clock += 1.days.inWholeMilliseconds + 1

        vm.openParty(vm.party("PSDB"))

        assertEquals(2, source.candidateCalls["psdb"])
    }

    @Test
    fun `recarregar a lista forca a lista e todos os partidos`() {
        val source = FakeSource()
        val vm = viewModel(source)
        val partyCalls = source.partyCalls

        vm.refreshAll()

        assertEquals(partyCalls + 1, source.partyCalls)
        assertEquals(2, source.candidateCalls["psdb"])
        assertEquals(2, source.candidateCalls["rep"])
        assertFalse(vm.state.value.isRefreshing)
        assertEquals(clock, vm.state.value.lastUpdated)
    }

    @Test
    fun `recarregar o partido forca so ele`() {
        val source = FakeSource(candidatesByParty = mapOf("psdb" to listOf(listOf(candidate("c1", "psdb")))))
        val vm = viewModel(source)
        vm.openParty(vm.party("PSDB"))

        vm.refreshParty()

        assertEquals(2, source.candidateCalls["psdb"])
        assertEquals(1, source.candidateCalls["rep"])
        assertEquals(listOf("c1"), vm.candidateList().map { it.id })
    }

    @Test
    fun `recarregar sem rede mantem os dados e avisa`() {
        val source = FakeSource()
        val vm = viewModel(source)
        source.failParties = AppError.NoConnection
        source.failCandidates = AppError.NoConnection

        vm.refreshAll()

        assertEquals(4, vm.partyList().size)
        assertTrue(vm.state.value.fromCache)
        assertFalse(vm.state.value.isRefreshing)
    }

    // --- Partido aberto: chips, paginação, vazio, voltar ---

    @Test
    fun `chip de cargo mantem o partido no filtro`() {
        val source = FakeSource(candidatesByParty = mapOf("psdb" to listOf(listOf(candidate("c1", "psdb")))))
        val vm = viewModel(source)
        vm.openParty(vm.party("PSDB"))

        vm.onCategoryChange(RankingCategory.SENATE)

        assertEquals(RankingCategory.SENATE, vm.state.value.category)
        assertEquals(setOf("psdb"), source.lastFilter?.partyIds)
        assertEquals(RankingCategory.SENATE.offices, source.lastFilter?.offices)
    }

    @Test
    fun `carregar mais junta a proxima pagina`() {
        val source = FakeSource(
            candidatesByParty = mapOf("pl" to listOf(listOf(candidate("c1", "pl")), listOf(candidate("c2", "pl")))),
            parties = PARTIES + PartyDto(id = "pl", number = 22, acronym = "PL", name = "PARTIDO LIBERAL"),
        )
        val vm = viewModel(source)
        vm.openParty(vm.party("PL"))

        vm.loadMore()

        assertEquals(listOf("c1", "c2"), vm.candidateList().map { it.id })
        assertEquals(true, vm.state.value.endReached)
    }

    @Test
    fun `partido sem candidatos vira estado vazio`() {
        val vm = viewModel(FakeSource())

        vm.openParty(vm.party("REPUBLICANOS"))

        assertEquals("Nenhuma candidatura desse partido em 2026", assertIs<UiState.Empty>(vm.state.value.candidates).message)

        vm.onCategoryChange(RankingCategory.SENATE)

        assertEquals("Nenhum candidato desse partido para esse cargo", assertIs<UiState.Empty>(vm.state.value.candidates).message)
    }

    @Test
    fun `partido novo da API aparece na lista e abre com os candidatos`() {
        val novo = PartyDto(id = "novo-99", number = 99, acronym = "PNF", name = "PARTIDO NOVO FUTURO")
        val source = FakeSource(
            parties = PARTIES + novo,
            candidatesByParty = mapOf("novo-99" to listOf(listOf(candidate("c9", "novo-99")))),
        )
        val vm = viewModel(source)

        val row = vm.party("PNF")
        assertEquals("PARTIDO NOVO FUTURO", row.name)
        assertEquals(PartyCount.Loaded(1), vm.state.value.counts["novo-99"])
        vm.openParty(row)

        assertEquals(listOf("c9"), vm.candidateList().map { it.id })
    }

    @Test
    fun `fechar o partido volta para a lista com a busca e a ordem`() {
        val vm = viewModel(FakeSource())
        vm.onQueryChange("psd")
        vm.onSortChange(PartySort.ACRONYM)
        vm.openParty(vm.party("PSDB"))

        vm.closeParty()

        assertNull(vm.state.value.selected)
        assertEquals("psd", vm.state.value.query)
        assertEquals(PartySort.ACRONYM, vm.state.value.sort)
        assertEquals(listOf("PSD", "PSDB"), vm.partyList().map { it.acronym })
    }

    private companion object {
        val PARTIES = listOf(
            PartyDto(id = "psd", number = 55, acronym = "PSD", name = "PARTIDO SOCIAL DEMOCRÁTICO"),
            PartyDto(id = "rep", number = 10, acronym = "REPUBLICANOS", name = "REPUBLICANOS"),
            PartyDto(id = "psdb", number = 45, acronym = "PSDB", name = "PARTIDO DA SOCIAL DEMOCRACIA BRASILEIRA"),
            PartyDto(id = "pdt", number = 12, acronym = "PDT", name = "PARTIDO DEMOCRÁTICO TRABALHISTA"),
            PartyDto(id = "psl", number = 17, acronym = "PSL", name = "PARTIDO SOCIAL LIBERAL"),
        )

        fun candidate(id: String, partyId: String) = CandidateDto(
            id = id, partyId = partyId, ballotName = id.uppercase(), office = "senator",
            officeLabel = "SENADOR", state = "SP", totalAssets = 100.0, assetsDisclosed = true,
        )
    }
}
