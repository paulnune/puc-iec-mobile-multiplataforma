package com.puciec.eleicoes.domain.mapa

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Cores do Mapa (#6), com as mesmas regras do eleicoes.dev/f/mapa: cada lugar fica na cor do partido
 * de quem lidera, mais clara quanto menor a margem. As cores são RGB no formato 0xRRGGBB, sem Compose,
 * para a regra ficar no commonMain e ser testada sem UI.
 */
object MapaColors {
    /** Fundo da página do site: a mistura parte dele quando a margem é pequena. */
    const val SITE_BASE: Int = 0xF7F8F6

    private val PARTY_COLORS: Map<String, Int> = mapOf(
        "PT" to 0xD71920,
        "PL" to 0x1F3F8B,
        "PSD" to 0xE0A100,
        "MDB" to 0x0E8A4B,
        "PP" to 0x0B74B8,
        "UNIAO" to 0x123A7A,
        "UNIÃO" to 0x123A7A,
        "REPUBLICANOS" to 0x163E78,
        "PSB" to 0xE36A12,
        "PDT" to 0xC62828,
        "PODE" to 0x1F6B45,
        "PSDB" to 0x0077C8,
        "NOVO" to 0xF15A22,
        "PSOL" to 0x5C068C,
        "PCDOB" to 0x9B0030,
        "PV" to 0x2E7D32,
        "REDE" to 0x00897B,
        "AVANTE" to 0xEF6C00,
        "SOLIDARIEDADE" to 0xF9A825,
        "PRD" to 0x01579B,
        "CIDADANIA" to 0xEF6C00,
        "MISSAO" to 0x6A1B9A,
        "MISSÃO" to 0x6A1B9A,
    )

    /** Partidos fora da tabela caem numa destas cores, sempre a mesma para a mesma sigla. */
    private val FALLBACK: IntArray = intArrayOf(0x455A64, 0x6D4C41, 0x00838F, 0xAD1457, 0x37474F, 0x827717)

    /** A margem a partir da qual o lugar já aparece com a cor pura do partido (28 pontos). */
    private const val FULL_MARGIN = 0.28

    fun partyColor(acronym: String?): Int {
        val key = acronym.orEmpty().trim().uppercase()
        PARTY_COLORS[key]?.let { return it }
        var hash = 0u
        for (char in key) hash = 33u * hash + char.code.toUInt()
        return FALLBACK[(hash % FALLBACK.size.toUInt()).toInt()]
    }

    /** Cor do lugar: [base] misturada à cor do partido, com intensidade de 0,18 (empate) a 1 (margem ≥ 28%). */
    fun leadFill(acronym: String?, leaderVotes: Long, runnerVotes: Long, base: Int = SITE_BASE): Int {
        val color = partyColor(acronym)
        val total = leaderVotes + runnerVotes
        if (total <= 0) return color
        val margin = maxOf(0.0, (leaderVotes - runnerVotes).toDouble() / total)
        val intensity = 0.18 + 0.82 * sqrt(minOf(1.0, margin / FULL_MARGIN))
        return mix(base, color, intensity)
    }

    private fun mix(from: Int, to: Int, amount: Double): Int {
        var rgb = 0
        for (shift in intArrayOf(16, 8, 0)) {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            // Mesmo arredondamento do Math.round do JavaScript.
            val channel = floor(a + (b - a) * amount + 0.5).toInt()
            rgb = rgb or (channel shl shift)
        }
        return rgb
    }
}
