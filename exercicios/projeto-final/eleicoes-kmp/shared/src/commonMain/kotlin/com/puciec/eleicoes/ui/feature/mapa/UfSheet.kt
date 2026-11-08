package com.puciec.eleicoes.ui.feature.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.puciec.eleicoes.ui.components.ErrorState
import com.puciec.eleicoes.ui.components.LoadingShimmer
import com.puciec.eleicoes.ui.state.UiState
import com.puciec.eleicoes.ui.theme.Spacing
import com.puciec.eleicoes.domain.mapa.MapaColors
import com.puciec.eleicoes.domain.mapa.Ufs
import com.puciec.eleicoes.domain.mapa.candidateRows
import com.puciec.eleicoes.domain.mapa.formatShare
import com.puciec.eleicoes.domain.mapa.formatVotes
import com.puciec.eleicoes.domain.mapa.placeSentence
import com.puciec.eleicoes.domain.mapa.toNameProse
import com.puciec.eleicoes.domain.model.PlaceResult
import com.puciec.eleicoes.domain.model.VoteCount

/** Nome do local no topo do sheet: o nome da UF com acento, ou "Exterior". */
internal fun placeLabel(code: String): String = Ufs.byCode(code)?.name ?: if (code == "ZZ") "Exterior" else code

/**
 * Votos por candidato de uma UF (#6, AD-002). Arrastar para baixo ou tocar fora fecha; o cargo
 * escolhido continua o mesmo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UfSheet(
    code: String,
    place: UiState<PlaceResult>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.l).padding(bottom = Spacing.xl)) {
            Text(placeLabel(code), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.m))
            when (place) {
                UiState.Loading -> LoadingShimmer(rows = 4)
                is UiState.Error -> Box(Modifier.height(320.dp)) { ErrorState(place.error, onRetry = onRetry) }
                is UiState.Empty -> Text("Sem votos para mostrar.", style = MaterialTheme.typography.bodyMedium)
                is UiState.Success -> PlaceVotes(place.data)
            }
        }
    }
}

@Composable
private fun PlaceVotes(place: PlaceResult) {
    Text(placeSentence(place), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(Spacing.l))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        items(candidateRows(place)) { CandidateVotesRow(it) }
    }
}

@Composable
private fun CandidateVotesRow(vote: VoteCount) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(rgbColor(MapaColors.partyColor(vote.partyAcronym)), CircleShape))
        Spacer(Modifier.size(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(
                vote.ballotName.orEmpty().toNameProse(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                vote.partyAcronym.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatVotes(vote.votes), style = MaterialTheme.typography.labelLarge)
            vote.voteShare?.let {
                Text(
                    formatShare(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
