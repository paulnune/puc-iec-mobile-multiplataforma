package com.puciec.eleicoes.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.puciec.eleicoes.ui.feature.evolucao.GainColorDark
import com.puciec.eleicoes.ui.feature.evolucao.GainColorLight
import com.puciec.eleicoes.ui.theme.DarkColors
import com.puciec.eleicoes.ui.theme.LightColors
import com.puciec.eleicoes.domain.a11y.Contrast
import kotlin.test.Test
import kotlin.test.assertTrue

/** Contraste das cores do tema no claro e no escuro (#11), pelo critério AA do WCAG (4,5:1 para texto). */
class ThemeContrastTest {

    private fun Color.rgb(): Int = toArgb() and 0xFFFFFF

    private fun assertReadable(name: String, text: Color, background: Color, min: Double = Contrast.AA_NORMAL_TEXT) {
        val ratio = Contrast.ratio(text.rgb(), background.rgb())
        assertTrue(ratio >= min, "$name: ${(ratio * 100).toInt() / 100.0}:1 (mínimo $min:1)")
    }

    private fun pairs(s: ColorScheme) = listOf(
        // Selos de resultado (OutcomeBadge): eleito, 2º turno, suplente, não eleito.
        Triple("selo eleito", s.onPrimaryContainer, s.primaryContainer),
        Triple("selo 2º turno", s.onSecondaryContainer, s.secondaryContainer),
        Triple("selo suplente", s.onTertiaryContainer, s.tertiaryContainer),
        Triple("selo não eleito", s.onSurfaceVariant, s.surfaceVariant),
        // Textos de apoio e valores sobre os cards e o fundo.
        Triple("texto secundário no fundo", s.onSurfaceVariant, s.surface),
        Triple("texto secundário no card", s.onSurfaceVariant, s.surfaceContainerLow),
        Triple("patrimônio (primary) no card", s.primary, s.surfaceContainerLow),
        Triple("perda (error) no fundo", s.error, s.surface),
        Triple("aviso de cache", s.onSecondaryContainer, s.secondaryContainer),
    )

    @Test
    fun `tema claro passa no AA`() {
        pairs(LightColors).forEach { (name, text, bg) -> assertReadable("claro, $name", text, bg) }
    }

    @Test
    fun `tema escuro passa no AA`() {
        pairs(DarkColors).forEach { (name, text, bg) -> assertReadable("escuro, $name", text, bg) }
    }

    @Test
    fun `verde de ganho da Evolucao legivel nos dois temas`() {
        assertReadable("ganho no claro", GainColorLight, LightColors.surface)
        assertReadable("ganho no escuro", GainColorDark, DarkColors.surface)
    }

    @Test
    fun `o verde antigo no escuro ficava abaixo do AA`() {
        // Documenta o problema corrigido: o verde do site sobre o fundo escuro.
        val ratio = Contrast.ratio(GainColorLight.rgb(), DarkColors.surface.rgb())
        assertTrue(ratio < Contrast.AA_NORMAL_TEXT, "razão $ratio")
    }
}
