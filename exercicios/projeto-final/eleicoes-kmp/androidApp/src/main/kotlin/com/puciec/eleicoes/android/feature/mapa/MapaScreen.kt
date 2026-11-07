package com.puciec.eleicoes.android.feature.mapa

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.navigation.MapaRoute
import com.puciec.eleicoes.android.ui.components.EmptyState

/**
 * Mapa do 1º turno (#6, Rodrigo). Dados: container.resultsRepository
 * (places para o Brasil e as UFs, place para os votos de uma UF). Só há 1º turno.
 */
fun NavGraphBuilder.mapaGraph(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    @Suppress("UNUSED_PARAMETER") container: AppContainer,
) {
    composable<MapaRoute> { MapaScreen() }
}

@Composable
fun MapaScreen() {
    // TODO(#6): resumo nacional, grade de UFs colorida pelo líder, Presidente/Governador e o detalhe da UF.
    EmptyState(title = "Mapa do 1º turno", message = "Em construção na issue #6.", icon = Icons.Filled.Place)
}
