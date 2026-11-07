package com.puciec.eleicoes.android.feature.evolucao

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.navigation.EvolucaoRoute
import com.puciec.eleicoes.android.ui.components.EmptyState

/**
 * Patrimônio entre eleições (#7, Gabriel). A tool list_wealth_changes está quebrada no servidor:
 * busque um grupo com candidatesRepository.ranking (ex.: Presidência), pegue a linha do tempo de cada um
 * com candidatesRepository.filings e calcule a variação em shared/.../domain/evolucao/.
 */
fun NavGraphBuilder.evolucaoGraph(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    @Suppress("UNUSED_PARAMETER") container: AppContainer,
) {
    composable<EvolucaoRoute> { EvolucaoScreen() }
}

@Composable
fun EvolucaoScreen() {
    // TODO(#7): ranking de variação (Maior variação, Mais ganhou, Mais perdeu) e o gráfico da linha do tempo.
    EmptyState(title = "Patrimônio entre eleições", message = "Em construção na issue #7.", icon = Icons.Filled.DateRange)
}
