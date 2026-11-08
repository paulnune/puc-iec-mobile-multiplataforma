package com.puciec.eleicoes.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
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
import com.puciec.eleicoes.domain.model.DataStatus

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
    // Paisagem: abas numa barra lateral, para sobrar altura para o conteúdo (#11).
    val windowSize = LocalWindowInfo.current.containerSize
    val useRail = windowSize.width > windowSize.height
    val labels = tabLabelStyle()
    val isSelected = { tab: TopLevelDestination -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }

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
                        onThemeChange = { container.setThemePreference(it) },
                    )
                },
            )
        },
        bottomBar = {
            if (!useRail) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        val selected = isSelected(tab)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = labels.iconDescription(tab, selected)) },
                            label = { TabLabel(tab, labels.style) },
                            alwaysShowLabel = labels.showAll,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (useRail) {
                // O Scaffold já descontou as barras do sistema no padding: a barra lateral não desconta de novo.
                NavigationRail(windowInsets = WindowInsets(0)) {
                    TopLevelDestination.entries.forEach { tab ->
                        val selected = isSelected(tab)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = labels.iconDescription(tab, selected)) },
                            label = { TabLabel(tab, labels.style) },
                            alwaysShowLabel = labels.showAll,
                        )
                    }
                }
            }
            AppContent(navController, container, dataStatus, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppContent(
    navController: NavHostController,
    container: AppContainer,
    dataStatus: DataStatus,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize()) {
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

/** A partir desta escala de fonte, só a aba selecionada mostra o nome; as outras ficam só com o ícone. */
private const val LARGE_FONT_SCALE = 1.3f

/**
 * Rótulos das abas com fonte grande (#11). Em 2× "Patrimônio" não cabe nos ~85 dp de uma aba e quebrava
 * no meio ("Patrim/ônio"). Aqui o rótulo cresce até 1,3× e não quebra; acima disso só a aba selecionada
 * mostra o nome, e as outras passam o nome ao TalkBack pelo ícone.
 */
private class TabLabels(val style: TextStyle, val showAll: Boolean) {
    fun iconDescription(tab: TopLevelDestination, selected: Boolean): String? =
        if (showAll || selected) null else tab.label
}

@Composable
private fun tabLabelStyle(): TabLabels {
    val fontScale = LocalDensity.current.fontScale
    val base = MaterialTheme.typography.labelMedium
    val style = if (fontScale > LARGE_FONT_SCALE) base.copy(fontSize = base.fontSize * (LARGE_FONT_SCALE / fontScale)) else base
    return TabLabels(style = style, showAll = fontScale <= LARGE_FONT_SCALE)
}

@Composable
private fun TabLabel(tab: TopLevelDestination, style: TextStyle) {
    Text(tab.label, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
}

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
