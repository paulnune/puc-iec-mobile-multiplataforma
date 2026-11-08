package com.puciec.eleicoes.ui.components

import com.puciec.eleicoes.platform.formatDayMonthTime
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.puciec.eleicoes.ui.theme.Spacing
import com.puciec.eleicoes.ui.theme.ThemePreference
import com.puciec.eleicoes.domain.model.DataStatus

/**
 * Faixa global abaixo da barra superior. Hoje some sempre (dados ao vivo);
 * o cache em disco (#12) publica [DataStatus.Cached] e ela aparece em todas as telas.
 */
@Composable
fun StatusBanner(status: DataStatus, modifier: Modifier = Modifier) {
    if (status !is DataStatus.Cached) return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            "Sem conexão: dados de ${formatDayMonthTime(status.savedAtEpochMillis)}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
        )
    }
}

/** Menu "⋮" da barra superior. */
@Composable
fun AppMenu(
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemePreference.entries.forEach { option ->
                val selected = option == themePreference
                DropdownMenuItem(
                    text = { Text("Tema: ${option.label}") },
                    leadingIcon = { if (selected) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onThemeChange(option)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Fonte dos dados") },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick = {
                    expanded = false
                    showAbout = true
                },
            )
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Fechar") } },
            title = { Text("Fonte dos dados") },
            text = {
                Text(
                    "Dados públicos do TSE das eleições de 2026, servidos pela API eleicoes.dev. " +
                        "O patrimônio é o que cada candidato declarou à Justiça Eleitoral.",
                )
            },
        )
    }
}
