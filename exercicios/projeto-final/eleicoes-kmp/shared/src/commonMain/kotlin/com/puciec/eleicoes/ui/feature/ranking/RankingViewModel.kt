package com.puciec.eleicoes.ui.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puciec.eleicoes.ui.AppContainer
import com.puciec.eleicoes.ui.state.UiState
import com.puciec.eleicoes.ui.state.toAppError
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.filter.matchingText
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

    /**
     * Busca da feature 3, local primeiro: quando o texto não acha ninguém na lista já carregada,
     * repete a consulta com `q` no servidor (#4). Assim "Cadu" no RN aparece sem precisar
     * carregar mais páginas. Com uma busca no servidor ativa, novos textos mantêm o `q` em dia;
     * apagar o texto limpa o `q` e recarrega a lista do filtro.
     */
    fun onSearchTextChange(text: String) {
        val current = _state.value
        _state.update { it.copy(searchText = text) }

        val query = text.trim()
        val serverQuery = current.filter.query
        if (query.isEmpty()) {
            if (serverQuery != null) {
                _state.update { it.copy(filter = it.filter.copy(query = null)) }
                load()
            }
            return
        }

        val loaded = (current.candidates as? UiState.Success)?.data.orEmpty()
        val hasLocalMatch = loaded.matchingText(query).isNotEmpty()

        when {
            // Já estamos buscando no servidor: o `q` acompanha o texto digitado.
            serverQuery != null -> if (serverQuery != query) {
                _state.update { it.copy(filter = it.filter.copy(query = query)) }
                load()
            }
            // Nada na lista carregada: a busca vira parâmetro da API em vez de mentir "não existe".
            !hasLocalMatch && current.candidates is UiState.Success -> {
                _state.update { it.copy(filter = it.filter.copy(query = query)) }
                load()
            }
        }
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
