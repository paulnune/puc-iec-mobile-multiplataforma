package com.puciec.eleicoes.ui.feature.evolucao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puciec.eleicoes.ui.AppContainer
import com.puciec.eleicoes.ui.state.UiState
import com.puciec.eleicoes.ui.state.loadState
import com.puciec.eleicoes.data.repository.EvolucaoRepository
import com.puciec.eleicoes.domain.evolucao.CandidateEvolucao
import com.puciec.eleicoes.domain.evolucao.RankingSort
import com.puciec.eleicoes.domain.evolucao.rankEvolucao
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.Office
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Os dois grupos da tela (#7): Presidência, ou Governadores e Senado de uma UF. */
enum class EvolucaoGroup { PRESIDENCY, GOVERNOR_SENATE }

data class EvolucaoUiState(
    val group: EvolucaoGroup = EvolucaoGroup.PRESIDENCY,
    /** UF usada quando o grupo é Governadores e Senado. */
    val uf: String = "SP",
    val sort: RankingSort = RankingSort.BIGGEST_CHANGE,
    val items: UiState<List<CandidateEvolucao>> = UiState.Loading,
    /** Candidato com a linha do tempo aberta no sheet; nulo com o sheet fechado. */
    val selected: CandidateEvolucao? = null,
) {
    private val loaded: List<CandidateEvolucao> get() = (items as? UiState.Success)?.data.orEmpty()

    /** Ranking do chip escolhido (só quem tem eleição anterior). */
    val ranking: List<CandidateEvolucao> get() = rankEvolucao(loaded, sort)

    /** "Sem eleição anterior": aparece à parte, fora do ranking. */
    val semHistorico: List<CandidateEvolucao> get() = loaded.filter { it.variation?.delta == null }
}

/** Filtro da API para cada grupo: Presidência, ou Governador e Senador de uma UF. */
internal fun groupFilter(group: EvolucaoGroup, uf: String): CandidateFilter = when (group) {
    EvolucaoGroup.PRESIDENCY -> CandidateFilter(offices = setOf(Office.PRESIDENT))
    EvolucaoGroup.GOVERNOR_SENATE -> CandidateFilter(offices = setOf(Office.GOVERNOR, Office.SENATOR), states = setOf(uf))
}

/** Patrimônio entre eleições (#7). O repositório busca; aqui só guarda o estado da tela. */
class EvolucaoViewModel(private val repository: EvolucaoRepository) : ViewModel() {
    private val _state = MutableStateFlow(EvolucaoUiState())
    val state: StateFlow<EvolucaoUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onGroupChange(group: EvolucaoGroup) {
        if (group == _state.value.group) return
        _state.update { it.copy(group = group, selected = null) }
        load()
    }

    fun onUfChange(uf: String) {
        if (uf == _state.value.uf) return
        _state.update { it.copy(uf = uf, selected = null) }
        if (_state.value.group == EvolucaoGroup.GOVERNOR_SENATE) load()
    }

    /** Chips Maior variação, Mais ganhou e Mais perdeu: só reordena o que já está carregado. */
    fun onSortChange(sort: RankingSort) {
        _state.update { it.copy(sort = sort) }
    }

    fun onSelect(candidateId: String) {
        val item = (_state.value.items as? UiState.Success)?.data?.firstOrNull { it.candidate.id == candidateId } ?: return
        _state.update { it.copy(selected = item) }
    }

    fun onSheetDismiss() {
        _state.update { it.copy(selected = null) }
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        val current = _state.value
        _state.update { it.copy(items = UiState.Loading) }
        loadJob = viewModelScope.launch {
            val result = loadState {
                repository.variations(groupFilter(current.group, current.uf))
            }
            _state.update { it.copy(items = result) }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { EvolucaoViewModel(container.evolucaoRepository) }
        }
    }
}
