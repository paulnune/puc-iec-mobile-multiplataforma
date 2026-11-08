package com.puciec.eleicoes.android.feature.detalhe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.feature.colinha.ColinhaAddDialog
import com.puciec.eleicoes.android.navigation.DetalheRoute
import com.puciec.eleicoes.android.ui.components.CandidatePhoto
import com.puciec.eleicoes.android.ui.components.EmptyState
import com.puciec.eleicoes.android.ui.components.ErrorState
import com.puciec.eleicoes.android.ui.components.LoadingShimmer
import com.puciec.eleicoes.android.ui.components.OutcomeBadge
import com.puciec.eleicoes.android.ui.components.subtitle
import com.puciec.eleicoes.android.ui.components.StarBorder
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.detalhe.AssetSummary
import com.puciec.eleicoes.domain.detalhe.DetalheFormat
import com.puciec.eleicoes.domain.detalhe.ageLabel
import com.puciec.eleicoes.domain.detalhe.birthplaceLabel
import com.puciec.eleicoes.domain.detalhe.shortAssetType
import com.puciec.eleicoes.domain.format.Money
import com.puciec.eleicoes.domain.format.toTitleCase
import com.puciec.eleicoes.domain.model.Asset
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.CandidateResult

fun NavGraphBuilder.detalheGraph(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    container: AppContainer,
) {
    composable<DetalheRoute> { entry ->
        val route: DetalheRoute = entry.toRoute()
        val viewModel: DetalheViewModel = viewModel(factory = DetalheViewModel.factory(route.candidateId, container))
        DetalheScreen(viewModel)
    }
}

@Composable
fun DetalheScreen(viewModel: DetalheViewModel) {
    val state by viewModel.candidate.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val pendingAdd by viewModel.pendingAdd.collectAsStateWithLifecycle()

    pendingAdd?.let {
        ColinhaAddDialog(it, onReplace = viewModel::confirmColinhaReplace, onDismiss = viewModel::dismissColinhaPrompt)
    }

    when (val candidate = state) {
        UiState.Loading -> LoadingShimmer(rows = 3)
        is UiState.Error -> ErrorState(candidate.error, onRetry = viewModel::retry)
        is UiState.Empty -> EmptyState(title = "Candidato não encontrado")
        is UiState.Success -> DetalheContent(
            candidate.data,
            isFavorite,
            assets,
            onToggleFavorite = viewModel::toggleFavorite,
            onRetryAssets = viewModel::retry,
        )
    }
}

@Composable
private fun DetalheContent(
    candidate: Candidate,
    isFavorite: Boolean,
    assets: UiState<AssetSummary>,
    onToggleFavorite: () -> Unit,
    onRetryAssets: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CandidatePhoto(candidate.photoUrl, candidate.ballotName, size = 88.dp, decorative = true)
            Spacer(Modifier.width(Spacing.l))
            Column(Modifier.weight(1f)) {
                Text(candidate.ballotName.toTitleCase(), style = MaterialTheme.typography.headlineSmall)
                Text(
                    listOfNotNull(
                        candidate.number.takeIf { it > 0 }?.let { "Nº $it" },
                        candidate.fullName.ifBlank { null }?.toTitleCase(),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    candidate.subtitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                OutcomeBadge(candidate.outcome)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else StarBorder,
                    contentDescription = if (isFavorite) "Remover da colinha" else "Adicionar à colinha",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            "Patrimônio declarado",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (candidate.assetsDisclosed) Money.full(candidate.totalAssets) else "Não declarado",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(Spacing.xl))
        ResultadoCard(candidate.result)

        Spacer(Modifier.height(Spacing.l))
        PerfilCard(candidate)

        Spacer(Modifier.height(Spacing.l))
        BensCard(assets, onRetry = onRetryAssets)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.s))
            content()
        }
    }
}

/** Linha "rótulo: valor" do perfil e do resultado. Quebra a linha em fonte grande em vez de cortar. */
@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ResultadoCard(result: CandidateResult?) {
    SectionCard("Resultado") {
        if (result == null) {
            Text("Sem resultado ainda", style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutcomeBadge(result.outcome)
            Spacer(Modifier.width(Spacing.s))
            Text(DetalheFormat.round(result.round), style = MaterialTheme.typography.bodyMedium)
        }
        result.votes?.let { InfoRow("Votos", DetalheFormat.votes(it)) }
        result.voteShare?.let { InfoRow("Votos válidos", DetalheFormat.percent(it)) }
    }
}

@Composable
private fun PerfilCard(candidate: Candidate) {
    val rows = listOfNotNull(
        candidate.ageLabel()?.let { "Idade" to it },
        candidate.birthplaceLabel()?.let { "Naturalidade" to it },
        candidate.education?.ifBlank { null }?.let { "Escolaridade" to it.toTitleCase() },
        candidate.occupation?.ifBlank { null }?.let { "Ocupação" to it.toTitleCase() },
    )
    SectionCard("Perfil") {
        if (rows.isEmpty()) {
            Text("Perfil não informado", style = MaterialTheme.typography.bodyMedium)
        } else {
            rows.forEach { (label, value) -> InfoRow(label, value) }
        }
    }
}

@Composable
private fun BensCard(state: UiState<AssetSummary>, onRetry: () -> Unit) {
    SectionCard("Bens declarados") {
        when (state) {
            UiState.Loading -> LoadingShimmer(rows = 2)
            is UiState.Error -> ErrorState(state.error, onRetry = onRetry)
            is UiState.Empty -> EmptyState(title = "Não declarou bens")
            is UiState.Success -> BensContent(state.data)
        }
    }
}

@Composable
private fun BensContent(summary: AssetSummary) {
    Text(
        Money.full(summary.total),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        if (summary.isTruncated) {
            "Mostrando ${summary.assets.size} de ${summary.declaredCount} bens"
        } else {
            "${summary.assets.size} ${if (summary.assets.size == 1) "bem" else "bens"}"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(Spacing.m))
    Text("Por tipo", style = MaterialTheme.typography.labelLarge)
    summary.groups.forEach { group ->
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${group.typeLabel} · ${group.count} ${if (group.count == 1) "bem" else "bens"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.s))
            Text(Money.compact(group.total), style = MaterialTheme.typography.bodyMedium)
        }
    }

    Spacer(Modifier.height(Spacing.m))
    HorizontalDivider()
    summary.assets.forEach { asset -> AssetRow(asset) }
}

@Composable
private fun AssetRow(asset: Asset) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.s), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(shortAssetType(asset.typeLabel), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(asset.description.ifBlank { "Sem descrição" }.toTitleCase(), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(Spacing.s))
        Text(Money.full(asset.value), style = MaterialTheme.typography.bodyMedium)
    }
}
