package com.puciec.eleicoes.android.feature.ranking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
 * Lista do ranking (F1, #2): posição, foto, nome, cargo, partido, UF, patrimônio e resultado.
 * Entra como itens na lista da tela, depois do cabeçalho, para tudo rolar junto: com o cabeçalho
 * fixo em cima, em paisagem ou com fonte grande ele ocupava a tela inteira e a lista sumia (#11).
 */
fun LazyListScope.rankingItems(
    candidates: List<Candidate>,
    onCandidateClick: (Candidate) -> Unit,
    isLoadingMore: Boolean,
    endReached: Boolean,
    loadMoreError: AppError?,
    onLoadMore: () -> Unit,
) {
    itemsIndexed(candidates, key = { _, candidate -> candidate.id }) { index, candidate ->
        CandidateCard(
            candidate,
            onClick = { onCandidateClick(candidate) },
            position = index + 1,
            modifier = Modifier.padding(horizontal = Spacing.l),
        )
    }
    item(key = "rodape") {
        Box(Modifier.padding(horizontal = Spacing.l)) {
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
 * Paginação infinita com prefetch: pede a próxima página quando faltam [PREFETCH_DISTANCE] cards.
 * [itemsBefore] são os itens antes do primeiro card (o cabeçalho). [autoLoadMore] fica desligado
 * durante a busca, para a lista curta não disparar página atrás de página (seriam até 700 chamadas à API).
 */
@Composable
fun RankingAutoLoad(
    listState: LazyListState,
    itemsBefore: Int,
    candidateCount: Int,
    autoLoadMore: Boolean,
    endReached: Boolean,
    loadMoreError: AppError?,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val nearEnd by remember(candidateCount, itemsBefore) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= itemsBefore + candidateCount - PREFETCH_DISTANCE
        }
    }
    // candidateCount na chave: se a página nova trouxe poucos itens e o fim continua à vista, pede a seguinte.
    LaunchedEffect(nearEnd, candidateCount, autoLoadMore, endReached, loadMoreError, isLoadingMore) {
        if (nearEnd && candidateCount > 0 && autoLoadMore && !endReached && loadMoreError == null && !isLoadingMore) onLoadMore()
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
