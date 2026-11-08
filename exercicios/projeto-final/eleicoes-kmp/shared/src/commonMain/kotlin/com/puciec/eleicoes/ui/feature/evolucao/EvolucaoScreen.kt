package com.puciec.eleicoes.ui.feature.evolucao

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.ui.AppContainer
import com.puciec.eleicoes.ui.navigation.EvolucaoRoute
import com.puciec.eleicoes.ui.components.EmptyState
import com.puciec.eleicoes.ui.components.ErrorState
import com.puciec.eleicoes.ui.components.LoadingShimmer
import com.puciec.eleicoes.ui.state.UiState
import com.puciec.eleicoes.ui.theme.Spacing
import com.puciec.eleicoes.domain.evolucao.CandidateEvolucao
import com.puciec.eleicoes.domain.evolucao.RankingSort
import com.puciec.eleicoes.domain.format.Money
import com.puciec.eleicoes.domain.mapa.Ufs
import com.puciec.eleicoes.domain.model.Filing
import kotlin.math.roundToInt

/**
 * Cor do ganho: verde do site no tema claro. No escuro esse verde fica em ~3,3:1 sobre o fundo; o verde
 * claro passa de 4,5:1 (#11). A perda usa a cor de erro do tema, que já é legível nos dois.
 */
internal val GainColorLight = Color(0xFF2E7D32)
internal val GainColorDark = Color(0xFF81C784)

@Composable
private fun gainColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) GainColorDark else GainColorLight

fun NavGraphBuilder.evolucaoGraph(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    container: AppContainer,
) {
    composable<EvolucaoRoute> {
        val viewModel: EvolucaoViewModel = viewModel(factory = EvolucaoViewModel.factory(container))
        EvolucaoScreen(viewModel)
    }
}

private val SORTS = listOf(
    RankingSort.BIGGEST_CHANGE to "Maior variação",
    RankingSort.MOST_GAINED to "Mais ganhou",
    RankingSort.MOST_LOST to "Mais perdeu",
)

@Composable
fun EvolucaoScreen(viewModel: EvolucaoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l, vertical = Spacing.xl),
    ) {
        Text("Patrimônio entre eleições", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(Spacing.s))
        Text(
            "Patrimônio declarado ao TSE, comparado com a eleição anterior.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.l))

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            FilterChip(
                selected = state.group == EvolucaoGroup.PRESIDENCY,
                onClick = { viewModel.onGroupChange(EvolucaoGroup.PRESIDENCY) },
                label = { Text("Presidência") },
            )
            FilterChip(
                selected = state.group == EvolucaoGroup.GOVERNOR_SENATE,
                onClick = { viewModel.onGroupChange(EvolucaoGroup.GOVERNOR_SENATE) },
                label = { Text("Governadores e Senado") },
            )
        }
        if (state.group == EvolucaoGroup.GOVERNOR_SENATE) {
            Spacer(Modifier.height(Spacing.s))
            UfChips(selected = state.uf, onSelect = viewModel::onUfChange)
        }
        Spacer(Modifier.height(Spacing.s))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SORTS.forEach { (sort, label) ->
                FilterChip(
                    selected = state.sort == sort,
                    onClick = { viewModel.onSortChange(sort) },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(Spacing.l))

        when (val items = state.items) {
            UiState.Loading -> LoadingShimmer()
            is UiState.Error -> ErrorState(items.error, onRetry = viewModel::retry)
            is UiState.Empty -> EmptyState(
                title = "Sem candidatos",
                message = "Nenhuma candidatura de 2026 neste grupo.",
                icon = Icons.Filled.DateRange,
            )
            is UiState.Success -> Ranking(
                ranking = state.ranking,
                semHistorico = state.semHistorico,
                onSelect = viewModel::onSelect,
            )
        }
    }

    state.selected?.let { item ->
        TimelineSheet(item = item, onDismiss = viewModel::onSheetDismiss)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UfChips(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Ufs.all.forEach { uf ->
            FilterChip(
                selected = uf.code == selected,
                onClick = { onSelect(uf.code) },
                label = { Text(uf.code) },
            )
        }
    }
}

@Composable
private fun Ranking(
    ranking: List<CandidateEvolucao>,
    semHistorico: List<CandidateEvolucao>,
    onSelect: (String) -> Unit,
) {
    if (ranking.isEmpty()) {
        Text("Nenhuma variação para mostrar neste grupo.", style = MaterialTheme.typography.bodyMedium)
    }
    ranking.forEach { item ->
        RankingRow(item = item, onClick = { onSelect(item.candidate.id) })
        HorizontalDivider()
    }
    if (semHistorico.isNotEmpty()) {
        Spacer(Modifier.height(Spacing.xl))
        Text("Sem eleição anterior", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.s))
        semHistorico.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp) // alvo de toque mínimo (#11)
                    .clickable { onSelect(item.candidate.id) }
                    .padding(vertical = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.candidate.ballotName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(Spacing.s))
                Text(item.candidate.state, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun RankingRow(item: CandidateEvolucao, onClick: () -> Unit) {
    val variation = item.variation ?: return
    val delta = variation.delta ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.candidate.ballotName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${item.candidate.state} · ${variation.previousYear} → ${variation.currentYear}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Money.signedCompact(delta),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (delta >= 0) gainColor() else MaterialTheme.colorScheme.error,
            )
            Text(
                ratioLabel(variation.ratio),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineSheet(item: CandidateEvolucao, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.l).padding(bottom = Spacing.xl)) {
            Text(item.candidate.ballotName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "Patrimônio declarado por eleição",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.l))
            TimelineChart(item.timeline, Modifier.fillMaxWidth().height(180.dp))
            if (item.variation?.delta == null) {
                Spacer(Modifier.height(Spacing.s))
                Text("Sem eleição anterior", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(Spacing.l))
            item.timeline.asReversed().forEach { filing -> FilingRow(filing) }
        }
    }
}

@Composable
private fun TimelineChart(timeline: List<Filing>, modifier: Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    // O Canvas não tem texto: o TalkBack lê um resumo do gráfico (os valores estão na lista abaixo) (#11).
    val description = "Gráfico do patrimônio declarado: " +
        timeline.joinToString("; ") { "${it.year}, ${Money.compact(it.totalAssets)}" }
    Canvas(modifier.semantics { contentDescription = description }) {
        val max = timeline.maxOfOrNull { it.totalAssets } ?: return@Canvas
        if (max <= 0.0) return@Canvas
        val points = timeline.mapIndexed { index, filing ->
            val x = if (timeline.size == 1) size.width / 2 else index * size.width / (timeline.size - 1)
            val y = size.height - (filing.totalAssets / max).toFloat() * size.height
            Offset(x, y)
        }
        val path = Path()
        points.forEachIndexed { index, point -> if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y) }
        drawPath(path, lineColor, style = Stroke(width = 4f))
        points.forEach { drawCircle(lineColor, radius = 6f, center = it) }
    }
}

@Composable
private fun FilingRow(filing: Filing) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(filing.year.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(filing.officeLabel, style = MaterialTheme.typography.bodyMedium)
            Text(filing.state, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Money.compact(filing.totalAssets), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Razão em porcentagem com sinal ("+38%", "−50%"); "—" quando não dá para calcular (patrimônio anterior zero). */
internal fun ratioLabel(ratio: Double?): String {
    if (ratio == null) return "—"
    val percent = (ratio * 100).roundToInt()
    return if (percent >= 0) "+$percent%" else "−${-percent}%"
}
