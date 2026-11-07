package com.puciec.eleicoes.android.feature.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.puciec.eleicoes.android.ui.components.CandidateCard
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.model.Candidate

/** Lista do ranking (F1, #2): posição, foto, nome, cargo, partido, UF, patrimônio e resultado. */
@Composable
fun RankingList(
    candidates: List<Candidate>,
    onCandidateClick: (Candidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        itemsIndexed(candidates, key = { _, candidate -> candidate.id }) { index, candidate ->
            CandidateCard(candidate, onClick = { onCandidateClick(candidate) }, position = index + 1)
        }
        // TODO(#2): ao chegar no fim, carregar a próxima página (paginação infinita).
    }
}
