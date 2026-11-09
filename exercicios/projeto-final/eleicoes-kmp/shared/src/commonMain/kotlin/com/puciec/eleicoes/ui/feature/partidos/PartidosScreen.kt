package com.puciec.eleicoes.ui.feature.partidos

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.ui.AppContainer
import com.puciec.eleicoes.ui.navigation.DetalheRoute
import com.puciec.eleicoes.ui.navigation.PartidosRoute
import com.puciec.eleicoes.domain.model.Candidate

/**
 * Partidos (#8). Lista dos 38 partidos com busca e ordem; tocar abre o partido com os candidatos
 * dele (chips de cargo, paginação) e tocar no candidato abre o Detalhe (#3).
 * O partido aberto é um modo do ViewModel, não uma rota: o "voltar" é tratado aqui pelo BackHandler.
 */
fun NavGraphBuilder.partidosGraph(
    navController: NavController,
    container: AppContainer,
) {
    composable<PartidosRoute> {
        val viewModel: PartidosViewModel = viewModel(factory = PartidosViewModel.factory(container))
        PartidosScreen(viewModel, onCandidateClick = { navController.navigate(DetalheRoute(it.id)) })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PartidosScreen(viewModel: PartidosViewModel, onCandidateClick: (Candidate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.selected != null) { viewModel.closeParty() }

    when (val party = state.selected) {
        null -> PartidosList(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onSortChange = viewModel::onSortChange,
            onPartyClick = viewModel::openParty,
            onRetry = viewModel::retry,
            onRefresh = viewModel::refreshAll,
        )
        else -> PartidoDetail(
            party = party,
            state = state,
            onBack = viewModel::closeParty,
            onCategoryChange = viewModel::onCategoryChange,
            onCandidateClick = onCandidateClick,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refreshParty,
            onRetry = viewModel::retry,
        )
    }
}
