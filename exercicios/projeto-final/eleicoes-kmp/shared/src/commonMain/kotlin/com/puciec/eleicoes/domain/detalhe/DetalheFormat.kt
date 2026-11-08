package com.puciec.eleicoes.domain.detalhe

import kotlin.math.abs
import kotlin.math.roundToLong

/** Números do Detalhe no padrão pt-BR. */
object DetalheFormat {

    /** "4.968.829 votos" ("1 voto" no singular). */
    fun votes(votes: Long): String = "${thousands(votes)} ${if (votes == 1L) "voto" else "votos"}"

    /**
     * Porcentagem de votos. A API já manda em escala 0–100 (24.208 = 24,2%), então não multiplica.
     * Uma casa decimal com vírgula; acima de zero e abaixo de 0,1 vira "< 0,1%".
     */
    fun percent(share: Double): String {
        if (share > 0.0 && share < 0.1) return "< 0,1%"
        val tenths = (abs(share) * 10).roundToLong()
        val sign = if (share < 0 && tenths != 0L) "-" else ""
        return "$sign${tenths / 10},${tenths % 10}%"
    }

    /** "1º turno" / "2º turno". */
    fun round(round: Int): String = "${round}º turno"

    private fun thousands(value: Long): String {
        val digits = abs(value).toString().reversed().chunked(3).joinToString(".").reversed()
        return if (value < 0) "-$digits" else digits
    }
}
