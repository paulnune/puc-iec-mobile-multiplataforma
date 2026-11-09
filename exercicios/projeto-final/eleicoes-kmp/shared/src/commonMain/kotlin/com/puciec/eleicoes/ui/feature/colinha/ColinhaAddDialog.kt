package com.puciec.eleicoes.ui.feature.colinha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.puciec.eleicoes.ui.theme.Spacing
import com.puciec.eleicoes.domain.colinha.AddDecision
import com.puciec.eleicoes.domain.favorites.FavoriteCandidate
import com.puciec.eleicoes.domain.format.toTitleCase

/** Pedido de favoritar que esbarrou numa regra da colinha e espera a resposta do usuário. */
data class PendingColinhaAdd(val candidate: FavoriteCandidate, val decision: AddDecision)

/**
 * Pergunta o que fazer quando o ★ esbarra numa regra da colinha: cargo cheio (qual sai?),
 * colinha de outra UF (trocar de UF?) ou cargo fora da urna (só avisa).
 * [onReplace] recebe quem sai da colinha para [PendingColinhaAdd.candidate] entrar.
 */
@Composable
fun ColinhaAddDialog(
    pending: PendingColinhaAdd,
    onReplace: (List<FavoriteCandidate>) -> Unit,
    onDismiss: () -> Unit,
) {
    val newName = pending.candidate.ballotName.toTitleCase()
    when (val decision = pending.decision) {
        is AddDecision.SeatFull -> SeatFullDialog(newName, decision.occupants, onReplace, onDismiss)

        is AddDecision.OtherState -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Colinha de ${decision.currentState}") },
            text = {
                Column {
                    Text(
                        "Você vota numa UF só. Para colar $newName (${decision.newState}), " +
                            "saem os candidatos de ${decision.currentState}:",
                    )
                    Spacer(Modifier.height(Spacing.s))
                    decision.toRemove.forEach {
                        Text(
                            "• ${it.ballotName.toTitleCase()} (${it.office.label})",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onReplace(decision.toRemove) }) { Text("Trocar para ${decision.newState}") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        )

        else -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Esse cargo não vai na urna") },
            text = { Text("Vice e suplentes não são votados em separado: entram junto com o titular da chapa.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Entendi") } },
        )
    }
}

@Composable
private fun SeatFullDialog(
    newName: String,
    occupants: List<FavoriteCandidate>,
    onReplace: (List<FavoriteCandidate>) -> Unit,
    onDismiss: () -> Unit,
) {
    // Com um ocupante só a escolha já está feita; senador (2 vagas) pede para escolher qual sai.
    var selected by remember(occupants) { mutableStateOf(occupants.singleOrNull()) }
    val office = occupants.firstOrNull()?.office?.label ?: "esse cargo"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Já tem $office na colinha") },
        text = {
            Column {
                if (occupants.size == 1) {
                    Text("Substituir ${occupants.first().ballotName.toTitleCase()} por $newName?")
                } else {
                    Text("As vagas de $office estão cheias. Quem sai para $newName entrar?")
                    Spacer(Modifier.height(Spacing.s))
                    Column(Modifier.selectableGroup()) {
                        occupants.forEach { occupant ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = occupant == selected,
                                        onClick = { selected = occupant },
                                        role = Role.RadioButton,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = occupant == selected, onClick = null)
                                Text(
                                    "${occupant.ballotName.toTitleCase()} · ${occupant.number}",
                                    modifier = Modifier.padding(start = Spacing.s),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onReplace(listOf(it)) } },
                enabled = selected != null,
            ) { Text("Substituir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
