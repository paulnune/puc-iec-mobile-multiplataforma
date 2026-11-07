package com.puciec.eleicoes.android.feature.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.puciec.eleicoes.android.ui.components.CandidateCard
import com.puciec.eleicoes.android.ui.components.LoadingShimmer
import com.puciec.eleicoes.android.ui.state.userMessage
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.model.AppError
import com.puciec.eleicoes.domain.model.Candidate

/** Quantos itens antes do fim a próxima página começa a carregar. */
private const val PREFETCH_DISTANCE = 5

/**
 * Lista do ranking (F1, #2): posição, foto, nome, cargo, partido, UF, patrimônio e resultado,
 * com paginação infinita. [autoLoadMore] fica desligado durante a busca, para a lista curta
 * não disparar página atrás de página (seriam até 700 chamadas à API).
 */
@Composable
fun RankingList(
    candidates: List<Candidate>,
    onCandidateClick: (Candidate) -> Unit,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    endReached: Boolean = true,
    loadMoreError: AppError? = null,
    autoLoadMore: Boolean = true,
    onLoadMore: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val nearEnd by remember(candidates.size) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= candidates.size - PREFETCH_DISTANCE
        }
    }
    // candidates.size na chave: se a página nova trouxe poucos itens e o fim continua à vista, pede a seguinte.
    LaunchedEffect(nearEnd, candidates.size, autoLoadMore, endReached, loadMoreError, isLoadingMore) {
        if (nearEnd && autoLoadMore && !endReached && loadMoreError == null && !isLoadingMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        itemsIndexed(candidates, key = { _, candidate -> candidate.id }) { index, candidate ->
            CandidateCard(candidate, onClick = { onCandidateClick(candidate) }, position = index + 1)
        }
        item(key = "rodape") {
            RankingFooter(
                count = candidates.size,
                isLoadingMore = isLoadingMore,
                endReached = endReached,
                error = loadMoreError,
                onLoadMore = onLoadMore,
            )
        }
    }
}

/**
 * Rodapé da lista: carregando mais, erro com nova tentativa, fim da lista ou o botão manual.
 * O botão aparece na busca e também como saída se a carga automática não disparar: a lista nunca fica sem caminho.
 */
@Composable
fun RankingFooter(
    count: Int,
    isLoadingMore: Boolean,
    endReached: Boolean,
    error: AppError?,
    onLoadMore: () -> Unit,
) {
    when {
        isLoadingMore -> LoadingShimmer(rows = 2)
        error != null -> Column(Modifier.fillMaxWidth().padding(Spacing.m), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                error.userMessage(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onLoadMore) { Text("Tentar de novo") }
        }
        endReached -> Box(Modifier.fillMaxWidth().padding(Spacing.l), contentAlignment = Alignment.Center) {
            Text(
                "Fim da lista · $count candidatos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onLoadMore) { Text("Carregar mais candidatos") }
        }
    }
}
