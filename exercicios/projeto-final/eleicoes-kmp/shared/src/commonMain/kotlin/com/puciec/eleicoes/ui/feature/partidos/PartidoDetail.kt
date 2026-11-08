package com.puciec.eleicoes.ui.feature.partidos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.puciec.eleicoes.ui.feature.ranking.RankingFooter
import com.puciec.eleicoes.ui.components.CandidateCard
import com.puciec.eleicoes.ui.components.EmptyState
import com.puciec.eleicoes.ui.components.ErrorState
import com.puciec.eleicoes.ui.components.LoadingShimmer
import com.puciec.eleicoes.ui.state.UiState
import com.puciec.eleicoes.ui.theme.Spacing
import com.puciec.eleicoes.domain.filter.RankingCategory
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Party
import com.puciec.eleicoes.domain.partidos.displayName

/** Quantos cards antes do fim a próxima página começa a carregar (mesmo valor do ranking). */
private const val PREFETCH_DISTANCE = 5

/**
 * Modo partido: cabeçalho, chips de cargo e os candidatos do maior para o menor patrimônio.
 * Tudo numa só lista rolável, para o cabeçalho não ocupar a tela inteira em paisagem ou com fonte grande.
 */
@Composable
fun PartidoDetail(
    party: Party,
    state: PartidosUiState,
    onBack: () -> Unit,
    onCategoryChange: (RankingCategory) -> Unit,
    onCandidateClick: (Candidate) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    val candidates = (state.candidates as? UiState.Success)?.data.orEmpty()
    val listState = rememberLazyListState()
    // Os 2 primeiros itens são o cabeçalho e os chips; os cards vêm depois.
    val nearEnd by remember(candidates.size) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= HEADER_ITEMS + candidates.size - PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(nearEnd, candidates.size, state.endReached, state.loadMoreError, state.isLoadingMore) {
        if (nearEnd && candidates.isNotEmpty() && !state.endReached && state.loadMoreError == null && !state.isLoadingMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        item(key = "cabecalho") { PartyHeader(party, state, onBack, onRefresh) }
        item(key = "chips") { CategoryChips(state.category, onCategoryChange) }

        when (val result = state.candidates) {
            UiState.Loading -> item(key = "carregando") { LoadingShimmer(rows = 6) }
            is UiState.Error -> item(key = "erro") { ErrorState(result.error, onRetry = onRetry) }
            is UiState.Empty -> item(key = "vazio") {
                EmptyState(
                    title = result.message ?: "Nenhum candidato",
                    message = if (state.category == RankingCategory.ALL) null else "Troque o cargo nos chips acima.",
                )
            }
            is UiState.Success -> {
                itemsIndexed(result.data, key = { _, c -> c.id }) { index, candidate ->
                    CandidateCard(
                        candidate,
                        onClick = { onCandidateClick(candidate) },
                        position = index + 1,
                        modifier = Modifier.padding(horizontal = Spacing.l),
                    )
                }
                item(key = "rodape") {
                    RankingFooter(
                        count = result.data.size,
                        isLoadingMore = state.isLoadingMore,
                        endReached = state.endReached,
                        error = state.loadMoreError,
                        onLoadMore = onLoadMore,
                    )
                }
            }
        }
    }
}

private const val HEADER_ITEMS = 2

@Composable
private fun PartyHeader(party: Party, state: PartidosUiState, onBack: () -> Unit, onRefresh: () -> Unit) {
    val total = state.total
    Row(
        Modifier.fillMaxWidth().padding(end = Spacing.l, top = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar para a lista de partidos")
        }
        Column(Modifier.weight(1f)) {
            Text("${party.number} · ${party.acronym}", style = MaterialTheme.typography.headlineSmall)
            party.displayName()?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            total?.let {
                Text(
                    candidaciesLabel(it),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state.fromCache) {
                Text(
                    "Sem conexão com a API: mostrando dados salvos no aparelho",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        RefreshButton(state.isRefreshing, onRefresh, "Recarregar este partido")
    }
}

/** "1.647 candidaturas" / "1 candidatura". */
internal fun candidaciesLabel(total: Int): String {
    val digits = total.toString().reversed().chunked(3).joinToString(".").reversed()
    return "$digits ${if (total == 1) "candidatura" else "candidaturas"}"
}

@Composable
private fun CategoryChips(selected: RankingCategory, onSelect: (RankingCategory) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        RankingCategory.entries.forEach { category ->
            FilterChip(selected = category == selected, onClick = { onSelect(category) }, label = { Text(category.label) })
        }
    }
}
