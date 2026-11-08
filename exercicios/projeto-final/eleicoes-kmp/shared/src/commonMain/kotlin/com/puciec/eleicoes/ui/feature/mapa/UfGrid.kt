package com.puciec.eleicoes.ui.feature.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.puciec.eleicoes.ui.theme.Spacing
import com.puciec.eleicoes.domain.a11y.Contrast
import com.puciec.eleicoes.domain.mapa.MapaColors
import com.puciec.eleicoes.domain.mapa.Uf
import com.puciec.eleicoes.domain.mapa.Ufs
import com.puciec.eleicoes.domain.mapa.toNameProse
import com.puciec.eleicoes.domain.model.PlaceResult

/** Converte o RGB do domínio (0xRRGGBB) numa cor opaca do Compose. */
internal fun rgbColor(rgb: Int): Color = Color(0xFF000000.toInt() or rgb)

private val MAX_TILE = 64.dp
private const val DARK_TEXT = 0x191C1A

/**
 * Grade de UFs do Mapa (#6): cada UF é um quadrado na posição aproximada do mapa (AD-001), pintado
 * com a cor do partido de quem lidera, mais forte quanto maior a margem (como no eleicoes.dev).
 */
@Composable
fun UfGrid(places: List<PlaceResult>, onUfClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val byCode = remember(places) { places.associateBy { it.code } }
    val surface = MaterialTheme.colorScheme.surface
    // O site só tem tema claro; no escuro a mistura parte do fundo do tema, senão a UF empatada fica quase branca.
    val base = if (surface.luminance() < 0.5f) surface.toArgb() and 0xFFFFFF else MapaColors.SITE_BASE
    val cells = remember { Ufs.all.associateBy { it.row to it.column } }

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gap = Spacing.xs
        val tile = minOf((maxWidth - gap * (Ufs.COLUMNS - 1)) / Ufs.COLUMNS, MAX_TILE)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until Ufs.ROWS) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (column in 0 until Ufs.COLUMNS) {
                        val uf = cells[row to column]
                        if (uf == null) {
                            Spacer(Modifier.size(tile))
                        } else {
                            UfTile(uf, byCode[uf.code], base, tile, onUfClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UfTile(uf: Uf, place: PlaceResult?, base: Int, size: Dp, onClick: (String) -> Unit) {
    val leader = place?.leader
    val fill = if (leader == null) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        rgbColor(MapaColors.leadFill(leader.partyAcronym, leader.votes, place.runnerUp?.votes ?: 0, base))
    }
    val description = if (leader == null) {
        "${uf.name}, sem dados"
    } else {
        "${uf.name}, ${leader.ballotName.orEmpty().toNameProse()} à frente"
    }
    val clickable = if (leader != null) {
        Modifier.clickable(role = Role.Button, onClickLabel = "Ver votos") { onClick(uf.code) }
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(fill)
            .then(clickable)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            uf.code,
            style = MaterialTheme.typography.labelLarge,
            // Negrito: em 14 sp vira "texto grande" no WCAG. A cor sai pelo contraste real (#11), e não por
            // luminância < 0,5, que punha branco em fundos médios com menos de 3:1.
            fontWeight = FontWeight.Bold,
            color = rgbColor(Contrast.readableOn(fill.toArgb() and 0xFFFFFF, dark = DARK_TEXT)),
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
