package com.puciec.eleicoes

import com.puciec.eleicoes.domain.a11y.Contrast
import com.puciec.eleicoes.domain.mapa.MapaColors
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContrastTest {

    private fun assertClose(expected: Double, actual: Double) =
        assertTrue(abs(expected - actual) < 0.01, "esperado $expected, veio $actual")

    @Test
    fun `preto e branco dao 21 e cores iguais dao 1`() {
        assertClose(21.0, Contrast.ratio(0x000000, 0xFFFFFF))
        assertClose(1.0, Contrast.ratio(0x0B6E4F, 0x0B6E4F))
    }

    @Test
    fun `razao bate com a calculadora do WCAG`() {
        // Valores de referência da WebAIM Contrast Checker.
        assertClose(4.54, Contrast.ratio(0x767676, 0xFFFFFF))
        assertClose(4.48, Contrast.ratio(0x777777, 0xFFFFFF)) // o cinza que não passa no AA
    }

    @Test
    fun `texto escuro em fundo medio, onde o branco falharia`() {
        // Cinza médio: luminância abaixo de 0,5, mas o branco fica abaixo de 3:1.
        val medio = 0x9E9E9E
        assertEquals(0x191C1A, Contrast.readableOn(medio))
        assertEquals(0xFFFFFF, Contrast.readableOn(0x1F3F8B))
    }

    @Test
    fun `todas as UFs do mapa ficam legiveis, do empate a margem cheia`() {
        val partidos = listOf("PT", "PL", "PSD", "MDB", "PP", "UNIÃO", "REPUBLICANOS", "PSB", "PDT", "PODE", "PSDB",
            "NOVO", "PSOL", "PCDOB", "PV", "REDE", "AVANTE", "SOLIDARIEDADE", "PRD", "CIDADANIA", "MISSÃO", "XYZ")
        val bases = listOf(MapaColors.SITE_BASE, 0x111412) // tema claro (site) e escuro
        for (base in bases) for (partido in partidos) for (votosDoSegundo in listOf(100L, 80L, 60L, 30L, 0L)) {
            val fill = MapaColors.leadFill(partido, 100, votosDoSegundo, base)
            val ratio = Contrast.ratio(fill, Contrast.readableOn(fill))
            // A sigla é 14 sp em negrito (texto grande no WCAG: 3:1). Com branco ou o texto escuro do tema,
            // o pior caso possível é ~4,1:1, então 4:1 vale para qualquer cor de fundo.
            assertTrue(ratio >= 4.0, "$partido sobre ${base.toString(16)} com margem $votosDoSegundo: $ratio")
        }
    }
}
