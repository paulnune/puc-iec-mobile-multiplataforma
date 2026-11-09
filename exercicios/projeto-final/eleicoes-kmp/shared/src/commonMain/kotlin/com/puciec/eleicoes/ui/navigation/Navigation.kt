package com.puciec.eleicoes.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// Rotas tipadas (Navigation Compose). As rotas de topo são as abas; cada feature
// declara as suas sub-rotas no próprio pacote e registra no seu *Graph.

@Serializable
data object RankingRoute

@Serializable
data object EvolucaoRoute

@Serializable
data object MapaRoute

@Serializable
data object PartidosRoute

@Serializable
data object ColinhaRoute

/** Detalhe do candidato: qualquer tela abre com navController.navigate(DetalheRoute(id)). */
@Serializable
data class DetalheRoute(val candidateId: String)

/** As 5 abas da barra inferior. */
enum class TopLevelDestination(val route: Any, val label: String, val icon: ImageVector) {
    RANKING(RankingRoute, "Patrimônio", Icons.Filled.Home),
    EVOLUCAO(EvolucaoRoute, "Evolução", Icons.Filled.DateRange),
    MAPA(MapaRoute, "Mapa", Icons.Filled.Place),
    PARTIDOS(PartidosRoute, "Partidos", Icons.AutoMirrored.Filled.List),
    COLINHA(ColinhaRoute, "Colinha", Icons.Filled.Star),
}
