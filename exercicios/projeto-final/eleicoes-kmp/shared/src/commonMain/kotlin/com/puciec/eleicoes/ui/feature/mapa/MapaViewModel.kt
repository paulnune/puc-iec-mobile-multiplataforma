package com.puciec.eleicoes.ui.feature.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puciec.eleicoes.ui.AppContainer
import com.puciec.eleicoes.ui.state.UiState
import com.puciec.eleicoes.ui.state.loadState
import com.puciec.eleicoes.data.repository.ResultsRepository
import com.puciec.eleicoes.domain.mapa.MapaOverview
import com.puciec.eleicoes.domain.mapa.buildMapaOverview
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceResult
import com.puciec.eleicoes.domain.model.PlaceScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapaUiState(
    /** Presidente ou Governador: os dois cargos que o Mapa mostra, como no site. */
    val office: Office = Office.PRESIDENT,
    val overview: UiState<MapaOverview> = UiState.Loading,
    /** UF aberta no bottom sheet ("SP", ou "ZZ" para o exterior); nulo com o sheet fechado. */
    val selectedCode: String? = null,
    val place: UiState<PlaceResult> = UiState.Loading,
)

/**
 * Mapa do 1º turno (#6). O repositório já pede sempre o 1º turno (`ResultsRepository.FIRST_ROUND`);
 * as regras de ordem, frase e legenda ficam no domínio (`buildMapaOverview`).
 */
class MapaViewModel(private val repository: ResultsRepository) : ViewModel() {
    private val _state = MutableStateFlow(MapaUiState())
    val state: StateFlow<MapaUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var placeJob: Job? = null

    init {
        load()
    }

    fun onOfficeChange(office: Office) {
        if (office == _state.value.office) return
        placeJob?.cancel()
        _state.update { it.copy(office = office, selectedCode = null) }
        load()
    }

    fun retry() = load()

    fun onPlaceSelected(code: String) {
        _state.update { it.copy(selectedCode = code) }
        loadPlace()
    }

    fun retryPlace() = loadPlace()

    fun onSheetDismiss() {
        placeJob?.cancel()
        _state.update { it.copy(selectedCode = null) }
    }

    private fun load() {
        loadJob?.cancel()
        val office = _state.value.office
        _state.update { it.copy(overview = UiState.Loading) }
        loadJob = viewModelScope.launch {
            val result = loadState(isEmpty = MapaOverview::isEmpty) {
                val national = if (office == Office.PRESIDENT) {
                    repository.places(office, PlaceScope.BRASIL).firstOrNull()
                } else {
                    null
                }
                buildMapaOverview(office, national, repository.places(office, PlaceScope.UF))
            }
            _state.update { it.copy(overview = result) }
        }
    }

    private fun loadPlace() {
        placeJob?.cancel()
        val code = _state.value.selectedCode ?: return
        val office = _state.value.office
        _state.update { it.copy(place = UiState.Loading) }
        placeJob = viewModelScope.launch {
            val result = loadState { repository.place(code, office, PlaceScope.UF) }
            _state.update { it.copy(place = result) }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MapaViewModel(container.resultsRepository) }
        }
    }
}
