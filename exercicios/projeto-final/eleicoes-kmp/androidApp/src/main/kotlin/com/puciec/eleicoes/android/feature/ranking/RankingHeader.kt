package com.puciec.eleicoes.android.feature.ranking

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.puciec.eleicoes.android.ui.components.CandidatePhoto
import com.puciec.eleicoes.android.ui.components.assetsLabel
import com.puciec.eleicoes.android.ui.components.subtitle
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.filter.RankingCategory
import com.puciec.eleicoes.domain.format.toTitleCase
import com.puciec.eleicoes.domain.mapa.Ufs
import com.puciec.eleicoes.domain.model.Candidate
import kotlinx.coroutines.delay

/** Espera antes de aplicar a busca: evita filtrar a cada tecla (feature 3). */
private const val SEARCH_DEBOUNCE_MS = 300L

/**
 * Cabeçalho da tela de patrimônio. Arquivo do #4 (Diego): busca, chips de categoria, UF e Hall da Fama.
 * A lista é do #2 e fica em RankingList.kt, então os dois trabalham em paralelo.
 */
@Composable
fun RankingHeader(
    filter: CandidateFilter,
    onFilterChange: (CandidateFilter) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    loadedCandidates: List<Candidate>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.m)) {
        Text("Patrimônio declarado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Candidaturas de 2026, do maior para o menor patrimônio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HallOfFame(loadedCandidates)

        CategoryChips(
            selected = RankingCategory.of(filter),
            onSelect = { category -> onFilterChange(category.toFilter(filter)) },
        )
        Spacer(Modifier.height(Spacing.m))

        DebouncedSearchField(
            value = searchText,
            onValueChange = onSearchTextChange,
        )
        Spacer(Modifier.height(Spacing.m))

        UfSelector(
            selected = filter.states.firstOrNull(),
            onSelect = { uf -> onFilterChange(filter.withState(uf)) },
        )
    }
}

/** Os 3 maiores patrimônios da consulta atual, roláveis na horizontal. */
@Composable
private fun HallOfFame(candidates: List<Candidate>) {
    val top = remember(candidates) { candidates.sortedByDescending { it.totalAssets }.take(3) }
    if (top.isEmpty()) return

    Spacer(Modifier.height(Spacing.l))
    Text("Hall da Fama", style = MaterialTheme.typography.titleLarge)
    Text(
        "Os 3 maiores patrimônios do filtro atual",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.m))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        contentPadding = PaddingValues(end = Spacing.l),
    ) {
        itemsIndexed(top, key = { _, candidate -> candidate.id }) { index, candidate ->
            HallCard(position = index + 1, candidate = candidate)
        }
    }
}

@Composable
private fun HallCard(position: Int, candidate: Candidate) {
    val first = position == 1
    val container = if (first) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val onContainer = if (first) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.width(250.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (first) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        "$position",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (first) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
                    )
                }
                Spacer(Modifier.width(Spacing.s))
                Text(
                    if (first) "Maior patrimônio" else "${position}º lugar",
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer,
                )
            }
            Spacer(Modifier.height(Spacing.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CandidatePhoto(candidate.photoUrl, candidate.ballotName, size = 48.dp, decorative = true)
                Spacer(Modifier.width(Spacing.s))
                Column(Modifier.weight(1f)) {
                    Text(
                        candidate.ballotName.toTitleCase(),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        candidate.subtitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.s))
            Text(
                candidate.assetsLabel(),
                style = MaterialTheme.typography.titleMedium,
                color = if (first) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CategoryChips(selected: RankingCategory, onSelect: (RankingCategory) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        RankingCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category.label) },
            )
        }
    }
}

/** Campo de busca com debounce local: o ViewModel só recebe o texto depois de uma pausa. */
@Composable
private fun DebouncedSearchField(value: String, onValueChange: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf(value) }

    LaunchedEffect(query) {
        if (query != value) {
            delay(SEARCH_DEBOUNCE_MS)
            onValueChange(query)
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar por nome") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpar busca")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
    )
}

/** Seletor de UF (`state`): "Todas" limpa o parâmetro e cada estado refaz a consulta. */
@Composable
private fun UfSelector(selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sortedUfs = remember { Ufs.all.sortedBy { it.name } }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "UF",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.s))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text(selected ?: "Todas as UFs")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Todas as UFs") },
                    onClick = {
                        expanded = false
                        onSelect(null)
                    },
                )
                sortedUfs.forEach { uf ->
                    DropdownMenuItem(
                        text = { Text("${uf.code} · ${uf.name}") },
                        onClick = {
                            expanded = false
                            onSelect(uf.code)
                        },
                        leadingIcon = if (uf.code == selected) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selecionada") }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
