package com.puciec.eleicoes.android.feature.colinha

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.navigation.ColinhaRoute
import com.puciec.eleicoes.android.navigation.DetalheRoute
import com.puciec.eleicoes.android.ui.components.CandidatePhoto
import com.puciec.eleicoes.android.ui.components.EmptyState
import com.puciec.eleicoes.android.ui.components.ErrorState
import com.puciec.eleicoes.android.ui.components.LoadingShimmer
import com.puciec.eleicoes.android.ui.components.OutcomeBadge
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.state.toAppError
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.colinha.Colinha
import com.puciec.eleicoes.domain.colinha.Scoreboard
import com.puciec.eleicoes.domain.favorites.FavoriteCandidate
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.format.toTitleCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun NavGraphBuilder.colinhaGraph(navController: NavController, container: AppContainer) {
    composable<ColinhaRoute> {
        val viewModel: ColinhaViewModel = viewModel(factory = ColinhaViewModel.factory(container))
        ColinhaScreen(viewModel, onCandidateClick = { navController.navigate(DetalheRoute(it.id)) })
    }
}

/** Uma linha da cola: o candidato, o lugar na urna ("Senador · 1ª vaga") e o número com os dígitos do cargo. */
data class ColinhaEntry(val favorite: FavoriteCandidate, val seatLabel: String, val digits: String)

data class ColinhaUi(
    val entries: List<ColinhaEntry>,
    val state: String?,
    val scoreboard: Scoreboard,
    val shareText: String,
) {
    companion object {
        fun from(favorites: List<FavoriteCandidate>): ColinhaUi {
            val ordered = Colinha.urnaOrder(favorites)
            return ColinhaUi(
                entries = ordered.map { ColinhaEntry(it, Colinha.seatLabel(it, ordered), Colinha.urnaDigits(it)) },
                state = Colinha.stateOf(ordered),
                scoreboard = Colinha.scoreboard(ordered),
                shareText = Colinha.shareText(ordered),
            )
        }
    }
}

/** Colinha (#5): favoritos gravados no aparelho, na ordem da urna, com o resultado de cada um. */
@OptIn(ExperimentalCoroutinesApi::class)
class ColinhaViewModel(private val favorites: FavoritesRepository) : ViewModel() {
    private val reload = MutableStateFlow(0)

    val state: StateFlow<UiState<ColinhaUi>> = reload
        .flatMapLatest {
            favorites.favorites
                .map { list ->
                    val ui = ColinhaUi.from(list)
                    if (ui.entries.isEmpty()) UiState.Empty() else UiState.Success(ui)
                }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun retry() = reload.update { it + 1 }

    fun remove(candidateId: String) {
        viewModelScope.launch { favorites.remove(candidateId) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ColinhaViewModel(container.favoritesRepository) }
        }
    }
}

@Composable
fun ColinhaScreen(viewModel: ColinhaViewModel, onCandidateClick: (FavoriteCandidate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val current = state) {
        UiState.Loading -> LoadingShimmer(rows = 4)
        is UiState.Error -> ErrorState(current.error, onRetry = viewModel::retry)
        is UiState.Empty -> EmptyState(
            title = "Sua colinha está vazia",
            message = "Favorite candidatos no detalhe (★) para montar a sua cola, na ordem da urna.",
            icon = Icons.Filled.Star,
        )
        is UiState.Success -> ColinhaContent(
            ui = current.data,
            onCandidateClick = onCandidateClick,
            onRemove = { viewModel.remove(it.id) },
            onShare = { context.shareText(current.data.shareText) },
        )
    }
}

@Composable
private fun ColinhaContent(
    ui: ColinhaUi,
    onCandidateClick: (FavoriteCandidate) -> Unit,
    onRemove: (FavoriteCandidate) -> Unit,
    onShare: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item(key = "header") { ColinhaHeader(ui, onShare) }
        items(ui.entries, key = { it.favorite.id }) { entry ->
            ColinhaRow(entry, onClick = { onCandidateClick(entry.favorite) }, onRemove = { onRemove(entry.favorite) })
        }
    }
}

@Composable
private fun ColinhaHeader(ui: ColinhaUi, onShare: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (ui.state != null) "Minha colinha · ${ui.state}" else "Minha colinha",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                ui.scoreboard.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.s))
        FilledTonalButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.s))
            Text("Compartilhar")
        }
    }
}

/** "2 de 5 eleitos · 1 no 2º turno": a eleição já aconteceu, então a colinha também é um placar. */
private fun Scoreboard.label(): String =
    "$elected de $total eleitos" + if (runoff > 0) " · $runoff no 2º turno" else ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColinhaRow(entry: ColinhaEntry, onClick: () -> Unit, onRemove: () -> Unit) {
    val favorite = entry.favorite
    val name = favorite.ballotName.toTitleCase()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.seatLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.s))
                UrnaKeys(entry.digits)
            }
            Spacer(Modifier.height(Spacing.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CandidatePhoto(favorite.photoUrl, favorite.ballotName, size = 48.dp, decorative = true)
                Spacer(Modifier.width(Spacing.m))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(favorite.partyAcronym, favorite.state.ifBlank { null }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    OutcomeBadge(favorite.outcome)
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Tirar $name da colinha",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** O número em "teclas" de urna, um dígito por tecla. */
@Composable
private fun UrnaKeys(digits: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = "Número ${digits.toList().joinToString(" ")}" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        digits.forEach { digit ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
            ) {
                // Tamanho mínimo, não fixo: com fonte grande a tecla cresce em vez de cortar o dígito (#11).
                Box(
                    Modifier.defaultMinSize(minWidth = 26.dp, minHeight = 34.dp).padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        digit.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

private fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, "Compartilhar colinha"))
}
