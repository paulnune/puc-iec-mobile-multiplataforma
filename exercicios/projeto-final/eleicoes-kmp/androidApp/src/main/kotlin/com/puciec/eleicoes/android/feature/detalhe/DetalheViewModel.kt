package com.puciec.eleicoes.android.feature.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.state.loadState
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.favorites.isFavorite
import com.puciec.eleicoes.domain.favorites.toFavorite
import com.puciec.eleicoes.domain.favorites.toggle
import com.puciec.eleicoes.domain.model.Candidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Detalhe do candidato (#3). O ★ usa a [FavoritesRepository]; a persistência vem do #5. */
class DetalheViewModel(
    private val candidateId: String,
    private val candidates: CandidatesRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {
    private val _candidate = MutableStateFlow<UiState<Candidate>>(UiState.Loading)
    val candidate: StateFlow<UiState<Candidate>> = _candidate.asStateFlow()

    val isFavorite: StateFlow<Boolean> = favorites.isFavorite(candidateId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // TODO(#3): bens declarados (candidates.assets) e o perfil completo.

    init {
        load()
    }

    fun retry() = load()

    fun toggleFavorite() {
        val current = (_candidate.value as? UiState.Success)?.data ?: return
        viewModelScope.launch { favorites.toggle(current.toFavorite()) }
    }

    private fun load() {
        _candidate.value = UiState.Loading
        viewModelScope.launch {
            _candidate.value = loadState(isEmpty = { false }) { candidates.candidate(candidateId) }
        }
    }

    companion object {
        fun factory(candidateId: String, container: AppContainer) = viewModelFactory {
            initializer { DetalheViewModel(candidateId, container.candidatesRepository, container.favoritesRepository) }
        }
    }
}
