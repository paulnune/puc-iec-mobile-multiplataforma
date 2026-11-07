package com.puciec.eleicoes.android.feature.ranking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.navigation.DetalheRoute
import com.puciec.eleicoes.android.navigation.RankingRoute
import com.puciec.eleicoes.android.ui.components.EmptyState
import com.puciec.eleicoes.android.ui.components.ErrorState
import com.puciec.eleicoes.android.ui.components.LoadingShimmer
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.domain.filter.matchingText
import com.puciec.eleicoes.domain.model.Candidate

fun NavGraphBuilder.rankingGraph(navController: NavController, container: AppContainer) {
    composable<RankingRoute> {
        val viewModel: RankingViewModel = viewModel(factory = RankingViewModel.factory(container))
        RankingScreen(viewModel, onCandidateClick = { navController.navigate(DetalheRoute(it.id)) })
    }
}

/** Tela de patrimônio: cabeçalho (#4) em cima, lista (#2) embaixo. */
@Composable
fun RankingScreen(viewModel: RankingViewModel, onCandidateClick: (Candidate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = (state.candidates as? UiState.Success)?.data.orEmpty()

    Column(Modifier.fillMaxSize()) {
        RankingHeader(
            filter = state.filter,
            onFilterChange = viewModel::onFilterChange,
            searchText = state.searchText,
            onSearchTextChange = viewModel::onSearchTextChange,
            loadedCandidates = loaded,
        )
        when (val candidates = state.candidates) {
            UiState.Loading -> LoadingShimmer()
            is UiState.Error -> ErrorState(candidates.error, onRetry = viewModel::retry)
            is UiState.Empty -> EmptyState(title = "Nenhum candidato para esse filtro")
            is UiState.Success -> {
                val visible = remember(candidates.data, state.searchText) { candidates.data.matchingText(state.searchText) }
                if (visible.isEmpty()) {
                    EmptyState(title = "Nenhum candidato com esse nome", message = "Tente outro nome ou limpe a busca.")
                } else {
                    RankingList(
                        candidates = visible,
                        onCandidateClick = onCandidateClick,
                        isLoadingMore = state.isLoadingMore,
                        endReached = state.endReached,
                        loadMoreError = state.loadMoreError,
                        // Com busca ativa a lista é curta: paginar sozinho varreria a API inteira.
                        autoLoadMore = state.searchText.isBlank(),
                        onLoadMore = viewModel::loadMore,
                    )
                }
            }
        }
    }
}
