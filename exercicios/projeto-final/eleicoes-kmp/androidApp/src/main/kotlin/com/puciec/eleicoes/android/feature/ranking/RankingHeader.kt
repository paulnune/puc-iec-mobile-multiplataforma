package com.puciec.eleicoes.android.feature.ranking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.model.Candidate

/**
 * Cabeçalho da tela de patrimônio. Arquivo do #4 (Diego): busca, chips de categoria, UF e Hall da Fama.
 * A lista é do #2 e fica em RankingList.kt, então os dois trabalham em paralelo.
 */
@Composable
fun RankingHeader(
    @Suppress("UNUSED_PARAMETER") filter: CandidateFilter,
    @Suppress("UNUSED_PARAMETER") onFilterChange: (CandidateFilter) -> Unit,
    @Suppress("UNUSED_PARAMETER") searchText: String,
    @Suppress("UNUSED_PARAMETER") onSearchTextChange: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") loadedCandidates: List<Candidate>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.m)) {
        Text("Patrimônio declarado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Candidaturas de 2026, do maior para o menor patrimônio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // TODO(#4): campo de busca (searchText, onSearchTextChange), chips de categoria e UF
        // (montam um CandidateFilter e chamam onFilterChange) e o Hall da Fama (top 3 de loadedCandidates).
    }
}
