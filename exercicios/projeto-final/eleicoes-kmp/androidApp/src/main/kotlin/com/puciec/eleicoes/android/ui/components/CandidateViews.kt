package com.puciec.eleicoes.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.format.Money
import com.puciec.eleicoes.domain.format.toTitleCase
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Outcome

/** "Governador · PL · SP" */
fun Candidate.subtitle(): String =
    listOfNotNull(officeLabel.toTitleCase().ifBlank { null }, partyAcronym, state.ifBlank { null }).joinToString(" · ")

/** Patrimônio curto ("R$ 8,2 mi") ou "Não declarado". */
fun Candidate.assetsLabel(): String = if (assetsDisclosed) Money.compact(totalAssets) else "Não declarado"

/**
 * Foto do candidato; sem foto, ou se a imagem falhar, mostra as iniciais.
 * [decorative]: quando o nome já aparece ao lado (cards, listas), o TalkBack pula a foto em vez de
 * ler "Foto de Fulano, FU, Fulano" (#11).
 */
@Composable
fun CandidatePhoto(
    url: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    decorative: Boolean = false,
) {
    val initials = remember(name) { name.initials() }
    val semantics = if (decorative) Modifier.clearAndSetSemantics { } else Modifier
    val shaped = modifier.size(size).clip(CircleShape).then(semantics)
    if (url.isNullOrBlank()) {
        InitialsAvatar(initials, shaped)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = if (decorative) null else "Foto de ${name.toTitleCase()}",
        modifier = shaped,
        contentScale = ContentScale.Crop,
        loading = { InitialsAvatar(initials, Modifier.fillMaxSize()) },
        error = { InitialsAvatar(initials, Modifier.fillMaxSize()) },
    )
}

@Composable
private fun InitialsAvatar(initials: String, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Text(initials, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

private fun String.initials(): String =
    split(" ")
        .filter { it.firstOrNull()?.isLetter() == true }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/** Selo do resultado da eleição. Cores do tema, legíveis no claro e no escuro. */
@Composable
fun OutcomeBadge(outcome: Outcome, modifier: Modifier = Modifier) {
    if (outcome == Outcome.UNKNOWN) return
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (outcome) {
        Outcome.ELECTED -> scheme.primaryContainer to scheme.onPrimaryContainer
        Outcome.RUNOFF -> scheme.secondaryContainer to scheme.onSecondaryContainer
        Outcome.ALTERNATE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        Outcome.NOT_ELECTED, Outcome.UNKNOWN -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    Surface(color = container, contentColor = content, shape = CircleShape, modifier = modifier) {
        Text(
            outcome.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = 2.dp),
        )
    }
}

/** Card padrão de candidato, usado no ranking, nos partidos e em outras listas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidateCard(
    candidate: Candidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: Int? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            if (position != null) {
                Text(
                    "$position",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 28.dp),
                )
            }
            CandidatePhoto(candidate.photoUrl, candidate.ballotName, decorative = true)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(
                    candidate.ballotName.toTitleCase(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                // Sem maxLines: com fonte grande o cargo, o partido e a UF quebram linha em vez de virar "…" (#11).
                Text(
                    candidate.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (candidate.outcome != Outcome.UNKNOWN) {
                    Spacer(Modifier.height(Spacing.xs))
                    OutcomeBadge(candidate.outcome)
                }
            }
            Spacer(Modifier.width(Spacing.s))
            Text(
                candidate.assetsLabel(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
            )
        }
    }
}
