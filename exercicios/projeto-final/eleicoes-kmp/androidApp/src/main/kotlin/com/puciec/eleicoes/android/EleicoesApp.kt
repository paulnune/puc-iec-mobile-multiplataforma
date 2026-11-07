package com.puciec.eleicoes.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.puciec.eleicoes.android.feature.colinha.colinhaGraph
import com.puciec.eleicoes.android.feature.detalhe.detalheGraph
import com.puciec.eleicoes.android.feature.evolucao.evolucaoGraph
import com.puciec.eleicoes.android.feature.mapa.mapaGraph
import com.puciec.eleicoes.android.feature.partidos.partidosGraph
import com.puciec.eleicoes.android.feature.ranking.rankingGraph
import com.puciec.eleicoes.android.navigation.RankingRoute
import com.puciec.eleicoes.android.navigation.TopLevelDestination
import com.puciec.eleicoes.android.ui.components.AppMenu
import com.puciec.eleicoes.android.ui.components.MissingKeyScreen
import com.puciec.eleicoes.android.ui.components.StatusBanner
import com.puciec.eleicoes.android.ui.theme.EleicoesTheme

@Composable
fun EleicoesApp(container: AppContainer) {
    val themePreference by container.themePreference.collectAsStateWithLifecycle()
    EleicoesTheme(themePreference = themePreference) {
        if (container.hasApiKey) EleicoesScaffold(container) else MissingKeyScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EleicoesScaffold(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val dataStatus by container.dataStatus.collectAsStateWithLifecycle()
    val themePreference by container.themePreference.collectAsStateWithLifecycle()
    val onTopLevel = destination == null ||
        TopLevelDestination.entries.any { tab -> destination.hasRoute(tab.route::class) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eleições 2026") },
                navigationIcon = {
                    if (!onTopLevel) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                },
                actions = {
                    AppMenu(
                        themePreference = themePreference,
                        onThemeChange = { container.themePreference.value = it },
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { tab ->
                    val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StatusBanner(dataStatus)
            // Cada feature registra as próprias rotas no seu *Graph: ninguém edita este arquivo.
            NavHost(
                navController = navController,
                startDestination = RankingRoute,
                modifier = Modifier.weight(1f),
            ) {
                rankingGraph(navController, container)
                detalheGraph(navController, container)
                evolucaoGraph(navController, container)
                mapaGraph(navController, container)
                partidosGraph(navController, container)
                colinhaGraph(navController, container)
            }
        }
    }
}

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
