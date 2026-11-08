package com.puciec.eleicoes.android.feature.partidos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.puciec.eleicoes.android.ui.components.EmptyState
import com.puciec.eleicoes.android.ui.components.ErrorState
import com.puciec.eleicoes.android.ui.components.LoadingShimmer
import com.puciec.eleicoes.android.ui.state.UiState
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.model.Party
import com.puciec.eleicoes.domain.partidos.PartySort
import com.puciec.eleicoes.domain.partidos.displayName
import com.puciec.eleicoes.domain.partidos.updatedAgo

/** Modo lista da aba Partidos: título, busca, ordem e os partidos. Tudo rola junto (paisagem e fonte grande). */
@Composable
fun PartidosList(
    state: PartidosUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (PartySort) -> Unit,
    onPartyClick: (Party) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        item(key = "cabecalho") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Partidos", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    RefreshButton(state.isRefreshing, onRefresh, "Recarregar partidos")
                }
                Text(
                    (state.parties as? UiState.Success)?.let { partiesSubtitle(it.data.size, state.query) }
                        ?: "Partidos de 2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FreshnessLine(state)
                if (state.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = Spacing.xs))
                Spacer(Modifier.height(Spacing.m))
                SearchField(state.query, onQueryChange)
                Spacer(Modifier.height(Spacing.s))
                SortChips(state.sort, onSortChange)
            }
        }

        when (val parties = state.parties) {
            UiState.Loading -> item(key = "carregando") { LoadingShimmer(rows = 6) }
            is UiState.Error -> item(key = "erro") { ErrorState(parties.error, onRetry = onRetry) }
            is UiState.Empty -> item(key = "vazio") {
                EmptyState(title = parties.message ?: "Nenhum partido", message = "Tente outra sigla, nome ou número.")
            }
            is UiState.Success -> items(parties.data, key = { it.id }) { party ->
                PartyRow(party, state.counts[party.id], onClick = { onPartyClick(party) })
            }
        }
    }
}

/** "Atualizado há 3 h" e, se a API falhou, o aviso de que a tela mostra o que estava salvo. */
@Composable
internal fun FreshnessLine(state: PartidosUiState) {
    val updated = state.lastUpdated ?: return
    Text(
        updatedAgo(updated, System.currentTimeMillis()),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.fromCache) {
        Text(
            "Sem conexão com a API: mostrando dados salvos no aparelho",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** Botão ↻ para forçar uma nova busca na API; vira um indicador enquanto recarrega. */
@Composable
internal fun RefreshButton(isRefreshing: Boolean, onClick: () -> Unit, description: String) {
    if (isRefreshing) {
        CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = "Recarregando" }, strokeWidth = 2.dp)
    } else {
        IconButton(onClick = onClick) { Icon(Icons.Filled.Refresh, contentDescription = description) }
    }
}

private fun partiesSubtitle(count: Int, query: String): String =
    if (query.isBlank()) "$count partidos de 2026" else "$count ${if (count == 1) "partido encontrado" else "partidos encontrados"}"

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar por sigla, nome ou número") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpar busca")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun SortChips(selected: PartySort, onSelect: (PartySort) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Ordenar por", style = MaterialTheme.typography.labelLarge)
        PartySort.entries.forEach { sort ->
            FilterChip(selected = sort == selected, onClick = { onSelect(sort) }, label = { Text(sort.label) })
        }
    }
}

/** Uma linha: número da urna, sigla, nome (só se for diferente da sigla) e, à direita, as candidaturas. */
@Composable
private fun PartyRow(party: Party, count: PartyCount?, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClickLabel = "Abrir partido", onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(Spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Text(
                party.number.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 48.dp),
            )
            Column(Modifier.padding(start = Spacing.m).weight(1f)) {
                Text(party.acronym, style = MaterialTheme.typography.titleMedium)
                party.displayName()?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            CandidaciesCount(count, Modifier.padding(start = Spacing.s))
        }
    }
}

/** "1.647 / candidaturas" à direita; "…" enquanto carrega; nada se a contagem falhou. */
@Composable
private fun CandidaciesCount(count: PartyCount?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        when (count) {
            is PartyCount.Loaded -> {
                Text(
                    thousands(count.total),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (count.total == 1) "candidatura" else "candidaturas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PartyCount.Loading, null -> Text(
                "…",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Contando candidaturas" },
            )
            PartyCount.Failed -> Unit
        }
    }
}

private fun thousands(value: Int): String = value.toString().reversed().chunked(3).joinToString(".").reversed()
