package com.puciec.eleicoes.domain.format

import kotlin.math.abs
import kotlin.math.roundToLong

/** Formatação de valores em reais, no padrão do site eleicoes.dev. */
object Money {
    private const val MINUS = "−"

    /** Forma curta: "R$ 1,23 bi", "R$ 242,6 mi", "R$ 795.089", "R$ 0". */
    fun compact(value: Double): String {
        val absolute = abs(value)
        val sign = if (value < 0) MINUS else ""
        val number = when {
            absolute >= 1_000_000_000 -> "${decimal(absolute / 1_000_000_000, 2)} bi"
            absolute >= 1_000_000 -> "${decimal(absolute / 1_000_000, 1)} mi"
            else -> thousands(absolute.roundToLong())
        }
        return "${sign}R$ $number"
    }

    /** Valor completo com centavos: "R$ 6.788.770,05". */
    fun full(value: Double): String {
        val sign = if (value < 0) MINUS else ""
        return "${sign}R$ ${decimal(abs(value), 2)}"
    }

    /** Variação com sinal: "+R$ 6,4 mi", "−R$ 2,6 mi". Zero sai sem sinal. */
    fun signedCompact(delta: Double): String = if (delta > 0) "+${compact(delta)}" else compact(delta)

    private fun decimal(value: Double, decimals: Int): String {
        var factor = 1L
        repeat(decimals) { factor *= 10 }
        val scaled = (value * factor).roundToLong()
        val integerPart = thousands(scaled / factor)
        if (decimals == 0) return integerPart
        val fraction = (scaled % factor).toString().padStart(decimals, '0')
        return "$integerPart,$fraction"
    }

    private fun thousands(value: Long): String =
        value.toString().reversed().chunked(3).joinToString(".").reversed()
}
