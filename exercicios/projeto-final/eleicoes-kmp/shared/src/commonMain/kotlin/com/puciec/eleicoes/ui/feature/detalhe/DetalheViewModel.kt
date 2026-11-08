package com.puciec.eleicoes.ui.feature.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.puciec.eleicoes.ui.AppContainer
import com.puciec.eleicoes.ui.feature.colinha.PendingColinhaAdd
import com.puciec.eleicoes.ui.state.UiState
import com.puciec.eleicoes.ui.state.loadState
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.domain.colinha.replace
import com.puciec.eleicoes.domain.colinha.toggleInColinha
import com.puciec.eleicoes.domain.detalhe.AssetSummary
import com.puciec.eleicoes.domain.detalhe.summarize
import com.puciec.eleicoes.domain.favorites.FavoriteCandidate
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.favorites.isFavorite
import com.puciec.eleicoes.domain.favorites.toFavorite
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

    /** ★ que esbarrou numa regra da colinha (cargo cheio, outra UF): a tela pergunta o que fazer. */
    private val _pendingAdd = MutableStateFlow<PendingColinhaAdd?>(null)
    val pendingAdd: StateFlow<PendingColinhaAdd?> = _pendingAdd.asStateFlow()

    /** Bens declarados, somados e agrupados por tipo. Carregam à parte: um erro aqui não derruba o cabeçalho. */
    private val _assets = MutableStateFlow<UiState<AssetSummary>>(UiState.Loading)
    val assets: StateFlow<UiState<AssetSummary>> = _assets.asStateFlow()

    init {
        load()
        loadAssets()
    }

    fun retry() {
        load()
        loadAssets()
    }

    fun toggleFavorite() {
        val current = (_candidate.value as? UiState.Success)?.data ?: return
        val favorite = current.toFavorite()
        viewModelScope.launch {
            favorites.toggleInColinha(favorite)?.let { _pendingAdd.value = PendingColinhaAdd(favorite, it) }
        }
    }

    /** O usuário aceitou tirar [toRemove] para o candidato entrar na colinha. */
    fun confirmColinhaReplace(toRemove: List<FavoriteCandidate>) {
        val pending = _pendingAdd.value ?: return
        _pendingAdd.value = null
        viewModelScope.launch { favorites.replace(toRemove, pending.candidate) }
    }

    fun dismissColinhaPrompt() {
        _pendingAdd.value = null
    }

    private fun load() {
        _candidate.value = UiState.Loading
        viewModelScope.launch {
            _candidate.value = loadState(isEmpty = { false }) { candidates.candidate(candidateId) }
        }
    }

    /** Busca todas as páginas de bens (até [MAX_ASSET_PAGES]) e resume. Lista vazia vira "não declarou bens". */
    private fun loadAssets() {
        _assets.value = UiState.Loading
        viewModelScope.launch {
            _assets.value = loadState(isEmpty = { it.assets.isEmpty() }) {
                val first = candidates.assets(candidateId, page = 1)
                val items = first.items.toMutableList()
                var page = first
                while (page.hasNext && page.page < MAX_ASSET_PAGES) {
                    page = candidates.assets(candidateId, page = page.page + 1)
                    items += page.items
                }
                items.summarize(declaredCount = first.total)
            }
        }
    }

    companion object {
        /** Teto de páginas de bens (50 por página): protege o limite de 60 req/min da chave. */
        const val MAX_ASSET_PAGES = 10

        fun factory(candidateId: String, container: AppContainer) = viewModelFactory {
            initializer { DetalheViewModel(candidateId, container.candidatesRepository, container.favoritesRepository) }
        }
    }
}
