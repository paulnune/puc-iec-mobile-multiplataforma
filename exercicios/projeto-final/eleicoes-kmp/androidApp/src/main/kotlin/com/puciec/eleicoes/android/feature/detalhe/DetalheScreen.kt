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
import androidx.compose.material.icons.outlined.Star
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
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.format.Money
import com.puciec.eleicoes.domain.format.toTitleCase
import com.puciec.eleicoes.domain.model.Candidate

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
    val pendingAdd by viewModel.pendingAdd.collectAsStateWithLifecycle()

    pendingAdd?.let {
        ColinhaAddDialog(it, onReplace = viewModel::confirmColinhaReplace, onDismiss = viewModel::dismissColinhaPrompt)
    }

    when (val candidate = state) {
        UiState.Loading -> LoadingShimmer(rows = 3)
        is UiState.Error -> ErrorState(candidate.error, onRetry = viewModel::retry)
        is UiState.Empty -> EmptyState(title = "Candidato não encontrado")
        is UiState.Success -> DetalheContent(candidate.data, isFavorite, onToggleFavorite = viewModel::toggleFavorite)
    }
}

@Composable
private fun DetalheContent(candidate: Candidate, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CandidatePhoto(candidate.photoUrl, candidate.ballotName, size = 88.dp)
            Spacer(Modifier.width(Spacing.l))
            Column(Modifier.weight(1f)) {
                Text(candidate.ballotName.toTitleCase(), style = MaterialTheme.typography.headlineSmall)
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
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
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
        // TODO(#3): perfil (idade, naturalidade, escolaridade, ocupação), votos e bens declarados.
        OutlinedCard(Modifier.fillMaxWidth()) {
            Text(
                "Perfil, votos e bens declarados: issue #3.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Spacing.l),
            )
        }
    }
}
