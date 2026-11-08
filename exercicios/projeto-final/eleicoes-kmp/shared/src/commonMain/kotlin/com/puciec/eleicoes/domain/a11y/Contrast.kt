package com.puciec.eleicoes.domain.a11y

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Contraste de cores pela fórmula do WCAG 2.1 (#11). Cores em RGB 0xRRGGBB, sem Compose,
 * para a regra ficar no commonMain e ser testada sem UI.
 *
 * Mínimos do WCAG AA: 4,5:1 para texto normal e 3:1 para texto grande (18 sp, ou 14 sp em negrito)
 * e para elementos gráficos.
 */
object Contrast {
    const val AA_NORMAL_TEXT = 4.5
    const val AA_LARGE_TEXT = 3.0

    /** Luminância relativa, de 0 (preto) a 1 (branco). */
    fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((rgb shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** Razão de contraste entre duas cores, de 1 (iguais) a 21 (preto e branco). */
    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /**
     * Das duas cores de texto, a que contrasta mais com [background]. Escolher pelo contraste (e não por
     * "luminância < 0,5") evita texto branco em fundos médios, onde ele fica abaixo de 3:1.
     */
    fun readableOn(background: Int, light: Int = 0xFFFFFF, dark: Int = 0x191C1A): Int =
        if (ratio(background, light) >= ratio(background, dark)) light else dark
}
