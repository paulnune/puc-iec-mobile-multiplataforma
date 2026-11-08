package com.puciec.eleicoes.ui.feature.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.ui.AppContainer
import com.puciec.eleicoes.ui.navigation.DetalheRoute
import com.puciec.eleicoes.ui.navigation.RankingRoute
import com.puciec.eleicoes.ui.components.EmptyState
import com.puciec.eleicoes.ui.components.ErrorState
import com.puciec.eleicoes.ui.components.LoadingShimmer
import com.puciec.eleicoes.ui.state.UiState
import com.puciec.eleicoes.ui.theme.Spacing
import com.puciec.eleicoes.domain.filter.matchingText
import com.puciec.eleicoes.domain.model.Candidate

fun NavGraphBuilder.rankingGraph(navController: NavController, container: AppContainer) {
    composable<RankingRoute> {
        val viewModel: RankingViewModel = viewModel(factory = RankingViewModel.factory(container))
        RankingScreen(viewModel, onCandidateClick = { navController.navigate(DetalheRoute(it.id)) })
    }
}

/** Itens antes do primeiro card: só o cabeçalho. */
private const val HEADER_ITEMS = 1

/**
 * Tela de patrimônio: cabeçalho (#4) e lista (#2) numa só lista rolável. O cabeçalho é sempre o
 * primeiro item, em qualquer estado, para não ser recriado (e o campo de busca não perder o foco)
 * quando a busca no servidor volta a carregar. Fixo em cima, ele ocupava a tela inteira em
 * paisagem ou com fonte grande (#11).
 */
@Composable
fun RankingScreen(viewModel: RankingViewModel, onCandidateClick: (Candidate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = (state.candidates as? UiState.Success)?.data.orEmpty()
    val visible = remember(loaded, state.searchText) { loaded.matchingText(state.searchText) }
    val listState = rememberLazyListState()

    RankingAutoLoad(
        listState = listState,
        itemsBefore = HEADER_ITEMS,
        candidateCount = visible.size,
        // Com busca ativa a lista é curta: paginar sozinho varreria a API inteira.
        autoLoadMore = state.searchText.isBlank(),
        endReached = state.endReached,
        loadMoreError = state.loadMoreError,
        isLoadingMore = state.isLoadingMore,
        onLoadMore = viewModel::loadMore,
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        item(key = "cabecalho") {
            RankingHeader(
                filter = state.filter,
                onFilterChange = viewModel::onFilterChange,
                searchText = state.searchText,
                onSearchTextChange = viewModel::onSearchTextChange,
                loadedCandidates = loaded,
            )
        }
        when (val candidates = state.candidates) {
            UiState.Loading -> item(key = "carregando") { LoadingShimmer() }
            is UiState.Error -> item(key = "erro") { ErrorState(candidates.error, onRetry = viewModel::retry) }
            is UiState.Empty -> item(key = "vazio") {
                EmptyState(
                    title = if (state.searchText.isBlank()) {
                        "Nenhum candidato para esse filtro"
                    } else {
                        "Nenhum candidato com esse nome"
                    },
                    message = if (state.searchText.isBlank()) null else "Tente outro nome ou limpe a busca.",
                )
            }
            is UiState.Success -> if (visible.isEmpty()) {
                item(key = "vazio") {
                    EmptyState(
                        title = "Nenhum candidato com esse nome",
                        message = "Tente outro nome ou limpe a busca.",
                        action = {
                            if (state.isLoadingMore || state.loadMoreError != null || !state.endReached) {
                                RankingFooter(
                                    count = candidates.data.size,
                                    isLoadingMore = state.isLoadingMore,
                                    endReached = state.endReached,
                                    error = state.loadMoreError,
                                    onLoadMore = viewModel::loadMore,
                                )
                            }
                        },
                    )
                }
            } else {
                rankingItems(
                    candidates = visible,
                    onCandidateClick = onCandidateClick,
                    isLoadingMore = state.isLoadingMore,
                    endReached = state.endReached,
                    loadMoreError = state.loadMoreError,
                    onLoadMore = viewModel::loadMore,
                )
            }
        }
    }
}
