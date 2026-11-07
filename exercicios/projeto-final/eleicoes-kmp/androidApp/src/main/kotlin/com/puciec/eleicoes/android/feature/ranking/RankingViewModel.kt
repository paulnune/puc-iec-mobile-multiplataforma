package com.puciec.eleicoes.android.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.state.toAppError
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.paging.PagedList
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankingUiState(
    /** Parâmetros reais da API, montados pelos chips e pela UF (#4). */
    val filter: CandidateFilter = CandidateFilter(),
    /** Texto da busca (#4): filtra no aparelho, sem nova consulta. */
    val searchText: String = "",
    /** Primeira página: carregando, erro, vazio ou a lista já acumulada. */
    val candidates: UiState<List<Candidate>> = UiState.Loading,
    /** Paginação infinita (#2). */
    val isLoadingMore: Boolean = false,
    val loadMoreError: AppError? = null,
    val endReached: Boolean = false,
    /** Total de candidaturas no filtro, quando a API informa. */
    val total: Int? = null,
)

/**
 * Padrão de referência do app: o repositório busca, o ViewModel guarda o estado num StateFlow,
 * a tela só desenha. Nenhum fetch dentro do composable.
 */
class RankingViewModel(private val repository: CandidatesRepository) : ViewModel() {
    private val _state = MutableStateFlow(RankingUiState())
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

    private var paged = PagedList<Candidate>()
    private var loadJob: Job? = null

    init {
        load()
    }

    /** Chamado pelo cabeçalho (#4) quando um chip ou a UF mudam: refaz a consulta na API. */
    fun onFilterChange(filter: CandidateFilter) {
        if (filter == _state.value.filter) return
        _state.update { it.copy(filter = filter) }
        load()
    }

    /** Busca da feature 3: só filtra a lista carregada. */
    fun onSearchTextChange(text: String) {
        _state.update { it.copy(searchText = text) }
    }

    fun retry() = load()

    /** Próxima página: a lista chama quando o usuário chega perto do fim (#2). */
    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || current.endReached || current.candidates !is UiState.Success) return
        val filter = current.filter
        val nextPage = paged.nextPage
        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        loadJob = viewModelScope.launch {
            try {
                val page = repository.ranking(filter, page = nextPage)
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

    /** Primeira página do filtro atual; descarta o que estava carregado. */
    private fun load() {
        loadJob?.cancel()
        paged = PagedList()
        val filter = _state.value.filter
        _state.update {
            it.copy(candidates = UiState.Loading, isLoadingMore = false, loadMoreError = null, endReached = false, total = null)
        }
        loadJob = viewModelScope.launch {
            try {
                val page = repository.ranking(filter, page = 1)
                paged = paged.append(page) { it.id }
                _state.update {
                    it.copy(
                        candidates = if (paged.items.isEmpty()) UiState.Empty() else UiState.Success(paged.items),
                        endReached = paged.endReached,
                        total = page.total,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(candidates = UiState.Error(e.toAppError())) }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { RankingViewModel(container.candidatesRepository) }
        }
    }
}
