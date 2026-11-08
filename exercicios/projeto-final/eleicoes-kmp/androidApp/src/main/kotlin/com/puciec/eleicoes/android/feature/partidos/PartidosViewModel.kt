package com.puciec.eleicoes.android.feature.partidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.state.toAppError
import com.puciec.eleicoes.data.cache.FileCacheStore
import com.puciec.eleicoes.data.partidos.PartidosCatalog
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.source.createEleicoesDataSource
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.filter.RankingCategory
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Page
import com.puciec.eleicoes.domain.model.Party
import com.puciec.eleicoes.domain.paging.PagedList
import com.puciec.eleicoes.domain.partidos.PartySort
import com.puciec.eleicoes.domain.partidos.filterAndSort
import com.puciec.eleicoes.domain.partidos.officialParties
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Quantas candidaturas o partido tem, mostrado à direita na lista (revisão 2). */
sealed interface PartyCount {
    data object Loading : PartyCount
    data class Loaded(val total: Int) : PartyCount
    data object Failed : PartyCount
}

/**
 * Estado da aba Partidos (#8). Dois modos na mesma tela: a lista ([selected] nulo) e o partido aberto.
 * Assim a tela do partido não precisa de rota nova no Navigation (arquivo compartilhado).
 */
data class PartidosUiState(
    /** Partidos já filtrados pela [query] e ordenados por [sort]. */
    val parties: UiState<List<Party>> = UiState.Loading,
    val query: String = "",
    val sort: PartySort = PartySort.NUMBER,
    val selected: Party? = null,
    val category: RankingCategory = RankingCategory.ALL,
    val candidates: UiState<List<Candidate>> = UiState.Loading,
    val total: Int? = null,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val loadMoreError: AppError? = null,
    /** Contagem por id do partido (pré-carregada ao abrir a aba). */
    val counts: Map<String, PartyCount> = emptyMap(),
    /** Botão de recarregar em andamento: os dados atuais continuam na tela. */
    val isRefreshing: Boolean = false,
    /** Quando a lista foi buscada na API (para "Atualizado há…"). */
    val lastUpdated: Long? = null,
    /** A API falhou e a tela está mostrando o que estava salvo no aparelho. */
    val fromCache: Boolean = false,
)

/**
 * Lista e partidos vêm do [catalog] (salvos no aparelho por 1 dia; revisão 8): ao abrir a aba, a lista e a
 * 1ª página de cada partido são pré-carregadas. Chips de cargo e "carregar mais" continuam ao vivo pelo
 * [candidatesRepository].
 */
class PartidosViewModel(
    private val catalog: PartidosCatalog,
    private val candidatesRepository: CandidatesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PartidosUiState())
    val state: StateFlow<PartidosUiState> = _state.asStateFlow()

    /** Os partidos de 2026 já corrigidos; busca e ordem são aplicadas no aparelho. */
    private var allParties: List<Party> = emptyList()
    private var partiesLoaded = false
    private var paged = PagedList<Candidate>()
    private var candidatesJob: Job? = null
    private var listJob: Job? = null

    /** No máximo 3 buscas ao mesmo tempo: a chave tem limite de 60 req/min. */
    private val permits = Semaphore(MAX_PARALLEL)

    init {
        loadAll(force = false)
    }

    fun onQueryChange(text: String) {
        _state.update { it.copy(query = text) }
        applyFilter()
    }

    fun onSortChange(sort: PartySort) {
        _state.update { it.copy(sort = sort) }
        applyFilter()
    }

    fun openParty(party: Party) {
        _state.update { it.copy(selected = party, category = RankingCategory.ALL) }
        loadCandidates(force = false)
    }

    fun onCategoryChange(category: RankingCategory) {
        if (_state.value.selected == null) return
        _state.update { it.copy(category = category) }
        loadCandidates(force = false)
    }

    /** Volta para a lista; a busca e a ordem ficam como estavam. */
    fun closeParty() {
        candidatesJob?.cancel()
        paged = PagedList()
        _state.update {
            it.copy(
                selected = null,
                category = RankingCategory.ALL,
                candidates = UiState.Loading,
                total = null,
                isLoadingMore = false,
                endReached = false,
                loadMoreError = null,
            )
        }
    }

    /** Refaz o que está na tela: os candidatos do partido aberto ou a lista de partidos. */
    fun retry() {
        if (_state.value.selected != null) loadCandidates(force = false) else loadAll(force = false)
    }

    /** Botão ↻ da lista: busca na API a lista e a 1ª página de todos os partidos, ignorando o prazo de 1 dia. */
    fun refreshAll() = loadAll(force = true)

    /** Botão ↻ do partido aberto: busca de novo só ele. */
    fun refreshParty() {
        if (_state.value.selected != null) loadCandidates(force = true)
    }

    /** Próxima página dos candidatos do partido (o PL tem mais de 1.600). */
    fun loadMore() {
        val current = _state.value
        if (current.selected == null || current.isLoadingMore || current.endReached || current.candidates !is UiState.Success) return
        val filter = current.filter()
        val nextPage = paged.nextPage
        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        candidatesJob = viewModelScope.launch {
            try {
                val page = candidatesRepository.ranking(filter, page = nextPage)
                paged = paged.append(page) { it.id }
                _state.update {
                    it.copy(
                        candidates = UiState.Success(paged.items),
                        isLoadingMore = false,
                        endReached = paged.endReached,
                        total = page.total ?: it.total,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMore = false, loadMoreError = e.toAppError()) }
            }
        }
    }

    /**
     * Lista + pré-carga da 1ª página de todos os partidos (contagens e lista de cada um prontas, também offline).
     * Com [force], é o recarregar: mantém os dados na tela e mostra só o indicador de progresso.
     */
    private fun loadAll(force: Boolean) {
        listJob?.cancel()
        val keepShowing = force && partiesLoaded
        _state.update { if (keepShowing) it.copy(isRefreshing = true) else it.copy(parties = UiState.Loading) }
        listJob = viewModelScope.launch {
            try {
                val result = catalog.parties(force)
                // Só os partidos de 2026, com sigla e nome oficiais (PCdoB, SOLIDARIEDADE, PRD com acento).
                allParties = result.value.officialParties()
                partiesLoaded = true
                _state.update { it.copy(lastUpdated = result.savedAtEpochMillis, fromCache = result.fromFallback) }
                applyFilter()
                prefetchAll(force)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!keepShowing) _state.update { it.copy(parties = UiState.Error(e.toAppError())) }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** 1ª página de cada partido (no máximo 3 por vez), que traz a contagem e deixa o partido pronto para abrir. */
    private suspend fun prefetchAll(force: Boolean) {
        allParties.map { party ->
            viewModelScope.async {
                if (_state.value.counts[party.id] !is PartyCount.Loaded) setCount(party.id, PartyCount.Loading)
                val count = try {
                    permits.withPermit {
                        val page = catalog.firstPage(party.id, force)
                        if (page.fromFallback) _state.update { it.copy(fromCache = true) }
                        PartyCount.Loaded(page.value.totalOrSize())
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.value.counts[party.id] as? PartyCount.Loaded ?: PartyCount.Failed
                }
                setCount(party.id, count)
            }
        }.awaitAll()
    }

    private fun setCount(partyId: String, count: PartyCount) {
        _state.update { it.copy(counts = it.counts + (partyId to count)) }
    }

    private fun applyFilter() {
        // Antes de os partidos chegarem (ou se falharam), a busca só guarda o texto.
        if (!partiesLoaded) return
        val s = _state.value
        val visible = allParties.filterAndSort(s.query, s.sort)
        _state.update {
            it.copy(parties = if (visible.isEmpty()) UiState.Empty("Nenhum partido para essa busca") else UiState.Success(visible))
        }
    }

    /**
     * Primeira página dos candidatos do partido aberto. No chip "Todos" vem do catálogo (salvo por 1 dia, ou
     * forçado pelo ↻); nos outros chips, ao vivo pelo repositório.
     */
    private fun loadCandidates(force: Boolean) {
        candidatesJob?.cancel()
        paged = PagedList()
        val current = _state.value
        val selected = current.selected ?: return
        val filter = current.filter()
        val fromCatalog = current.category == RankingCategory.ALL
        _state.update {
            it.copy(
                candidates = if (force && it.candidates is UiState.Success) it.candidates else UiState.Loading,
                isRefreshing = force,
                isLoadingMore = false,
                endReached = false,
                loadMoreError = null,
            )
        }
        candidatesJob = viewModelScope.launch {
            try {
                val page: Page<Candidate> = if (fromCatalog) {
                    val cached = catalog.firstPage(selected.id, force)
                    if (cached.fromFallback) _state.update { it.copy(fromCache = true) }
                    cached.value
                } else {
                    candidatesRepository.ranking(filter, page = 1)
                }
                paged = paged.append(page) { it.id }
                _state.update {
                    it.copy(
                        candidates = if (paged.items.isEmpty()) {
                            UiState.Empty(
                                if (it.category == RankingCategory.ALL) {
                                    "Nenhuma candidatura desse partido em 2026"
                                } else {
                                    "Nenhum candidato desse partido para esse cargo"
                                },
                            )
                        } else {
                            UiState.Success(paged.items)
                        },
                        endReached = paged.endReached,
                        total = page.total,
                    )
                }
                if (fromCatalog) setCount(selected.id, PartyCount.Loaded(page.totalOrSize()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(candidates = UiState.Error(e.toAppError())) }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun Page<Candidate>.totalOrSize(): Int = total ?: items.size

    /** Filtro enviado à API: o partido aberto + os cargos do chip (parâmetros reais `partyId` e `office`). */
    private fun PartidosUiState.filter(): CandidateFilter =
        category.toFilter(CandidateFilter(partyIds = setOfNotNull(selected?.id)))

    companion object {
        private const val MAX_PARALLEL = 3

        /**
         * O catálogo lê a API sem o cache de 5 min do resto do app (o ↻ precisa buscar de verdade) e grava numa
         * pasta própria, separada da do cache offline (#12). O AppContainer não muda.
         */
        fun factory(container: AppContainer, filesDir: File) = viewModelFactory {
            initializer {
                PartidosViewModel(
                    catalog = PartidosCatalog(
                        source = createEleicoesDataSource(),
                        store = FileCacheStore(File(filesDir, "partidos-cache")),
                        now = System::currentTimeMillis,
                    ),
                    candidatesRepository = container.candidatesRepository,
                )
            }
        }
    }
}
