package com.puciec.eleicoes.android.feature.partidos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.navigation.PartidosRoute
import com.puciec.eleicoes.android.ui.components.EmptyState

/**
 * Partidos (#8, José Ricardo). Dados: container.partiesRepository (all, party) e
 * container.candidatesRepository.ranking com CandidateFilter(partyIds = setOf(id)).
 * Para abrir um candidato: navController.navigate(DetalheRoute(id)).
 */
fun NavGraphBuilder.partidosGraph(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    @Suppress("UNUSED_PARAMETER") container: AppContainer,
) {
    composable<PartidosRoute> { PartidosScreen() }
}

@Composable
fun PartidosScreen() {
    // TODO(#8): lista de partidos com busca e a tela do partido com os candidatos dele.
    EmptyState(title = "Partidos", message = "Em construção na issue #8.", icon = Icons.AutoMirrored.Filled.List)
}
