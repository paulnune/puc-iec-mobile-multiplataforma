package com.puciec.eleicoes.android.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.state.loadState
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.Candidate
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
    val candidates: UiState<List<Candidate>> = UiState.Loading,
    // TODO(#2): paginação (página atual, "carregando mais" e fim da lista).
)

/**
 * Padrão de referência do app: o repositório busca, o ViewModel guarda o estado num StateFlow,
 * a tela só desenha. Nenhum fetch dentro do composable.
 */
class RankingViewModel(private val repository: CandidatesRepository) : ViewModel() {
    private val _state = MutableStateFlow(RankingUiState())
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

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

    private fun load() {
        loadJob?.cancel()
        val filter = _state.value.filter
        _state.update { it.copy(candidates = UiState.Loading) }
        loadJob = viewModelScope.launch {
            val result = loadState { repository.ranking(filter, page = 1).items }
            _state.update { it.copy(candidates = result) }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { RankingViewModel(container.candidatesRepository) }
        }
    }
}
