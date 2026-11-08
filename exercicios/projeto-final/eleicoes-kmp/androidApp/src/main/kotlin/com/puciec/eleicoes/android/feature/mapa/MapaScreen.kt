package com.puciec.eleicoes.android.feature.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.navigation.MapaRoute
import com.puciec.eleicoes.android.ui.components.EmptyState
import com.puciec.eleicoes.android.ui.components.ErrorState
import com.puciec.eleicoes.android.ui.components.LoadingShimmer
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.mapa.MAPA_EYEBROW
import com.puciec.eleicoes.domain.mapa.MAPA_TITLE
import com.puciec.eleicoes.domain.mapa.MapaColors
import com.puciec.eleicoes.domain.mapa.MapaOverview
import com.puciec.eleicoes.domain.mapa.SOURCE_NOTE
import com.puciec.eleicoes.domain.mapa.formatVotes
import com.puciec.eleicoes.domain.mapa.toNameProse
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceResult

/**
 * Mapa do 1º turno (#6, Rodrigo): resumo nacional, lista de estados, grade de UFs colorida pelo líder,
 * Presidente ou Governador, e os votos de cada UF num bottom sheet. Padrão visual do eleicoes.dev/f/mapa.
 */
fun NavGraphBuilder.mapaGraph(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    container: AppContainer,
) {
    composable<MapaRoute> {
        val viewModel: MapaViewModel = viewModel(factory = MapaViewModel.factory(container))
        MapaScreen(viewModel)
    }
}

private val OFFICES = listOf(Office.PRESIDENT to "Presidente", Office.GOVERNOR to "Governador")

/** Altura da lista "Estados": cerca de 8 linhas, com rolagem própria, como no site. */
private val STATES_LIST_HEIGHT = 440.dp

/** Em paisagem a tela útil tem ~250 dp: a lista fica com no máximo metade da altura, para não prender a rolagem (#11). */
private const val STATES_LIST_MAX_SCREEN_FRACTION = 0.5f

@Composable
fun MapaScreen(viewModel: MapaViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l, vertical = Spacing.xl),
    ) {
        Text(
            MAPA_EYEBROW,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.s))
        Text(MAPA_TITLE, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(Spacing.xl))
        OfficeToggle(selected = state.office, onSelect = viewModel::onOfficeChange)
        Spacer(Modifier.height(Spacing.xl))

        when (val overview = state.overview) {
            UiState.Loading -> LoadingShimmer()
            is UiState.Error -> ErrorState(overview.error, onRetry = viewModel::retry)
            is UiState.Empty -> EmptyState(
                title = "Sem resultados",
                message = "A API ainda não tem a totalização do 1º turno para este cargo.",
                icon = Icons.Filled.Place,
            )
            is UiState.Success -> MapaContent(overview.data, onPlaceClick = viewModel::onPlaceSelected)
        }
    }

    state.selectedCode?.let { code ->
        UfSheet(code = code, place = state.place, onRetry = viewModel::retryPlace, onDismiss = viewModel::onSheetDismiss)
    }
}

@Composable
private fun OfficeToggle(selected: Office, onSelect: (Office) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        OFFICES.forEach { (office, label) ->
            val isSelected = office == selected
            Surface(
                onClick = { onSelect(office) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { this.selected = isSelected },
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapaContent(overview: MapaOverview, onPlaceClick: (String) -> Unit) {
    Text(overview.headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(Spacing.xl))

    Text("Estados", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(Spacing.s))
    val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val listHeight = minOf(STATES_LIST_HEIGHT, screenHeight * STATES_LIST_MAX_SCREEN_FRACTION)
    Column(Modifier.heightIn(max = listHeight).verticalScroll(rememberScrollState())) {
        overview.states.forEach { place ->
            PlaceRow(label = placeLabel(place.code), place = place, onClick = { onPlaceClick(place.code) })
        }
    }
    overview.exterior?.let { exterior ->
        HorizontalDivider(Modifier.padding(vertical = Spacing.s))
        PlaceRow(label = placeLabel(exterior.code), place = exterior, onClick = { onPlaceClick(exterior.code) })
    }

    Spacer(Modifier.height(Spacing.xl))
    // UFs sem líder (overview.noData) não estão em states: a grade as desenha cinza, com "sem dados".
    UfGrid(places = overview.states, onUfClick = onPlaceClick)

    Spacer(Modifier.height(Spacing.l))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        overview.legend.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PartyDot(item.partyAcronym)
                Spacer(Modifier.size(Spacing.s))
                Text("${item.leaderName} · ${item.partyAcronym}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    Spacer(Modifier.height(Spacing.xl))
    Text(
        SOURCE_NOTE,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PlaceRow(label: String, place: PlaceResult, onClick: () -> Unit) {
    val leader = place.leader ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.s, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PartyDot(leader.partyAcronym)
        Spacer(Modifier.size(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${leader.ballotName.orEmpty().toNameProse()} · ${leader.partyAcronym.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatVotes(leader.votes),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PartyDot(acronym: String?) {
    Box(Modifier.size(10.dp).background(rgbColor(MapaColors.partyColor(acronym)), CircleShape))
}
