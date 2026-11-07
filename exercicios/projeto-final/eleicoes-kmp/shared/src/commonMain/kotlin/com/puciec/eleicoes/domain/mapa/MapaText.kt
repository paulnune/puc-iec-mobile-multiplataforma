package com.puciec.eleicoes.domain.mapa

import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceResult
import com.puciec.eleicoes.domain.model.PlaceScope
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/** Textos do Mapa (#6), no formato do eleicoes.dev/f/mapa. */
const val MAPA_EYEBROW = "1º TURNO · 2026"
const val MAPA_TITLE = "O mapa do primeiro turno"
const val GOVERNOR_HINT = "Toque um estado. A cor mostra quem teve mais votos na disputa daquele estado."
const val SOURCE_NOTE =
    "Fonte: TSE, totalização do 1º turno, arquivos fechados em 5 de outubro de 2026. " +
        "A cor mostra quem teve mais votos naquele lugar."

private const val EXTERIOR = "ZZ"
private const val VOWELS = "aeiouáàâãéêíóôõúü"
private val WORD = Regex("\\S+")

/**
 * Nome como o site escreve: cada palavra com inicial maiúscula, exceto siglas de 2 a 4 letras
 * sem vogal ("ACM NETO" → "ACM Neto", "JHC" → "JHC", "DR. FURLAN" → "Dr. Furlan").
 */
fun String.toNameProse(): String = WORD.replace(this) { match ->
    val word = match.value
    val isAcronym = word.length in 2..4 && word.all { it.isLetter() } && word.none { it.lowercaseChar() in VOWELS }
    if (isAcronym) {
        word.uppercase()
    } else {
        val lower = word.lowercase()
        if (lower.first().isLetter()) lower.replaceFirstChar { it.uppercaseChar() } else lower
    }
}

/** Votos com ponto de milhar: "56.104.503". */
fun formatVotes(votes: Long): String =
    votes.toString().reversed().chunked(3).joinToString(".").reversed()

/** Percentual com uma casa e vírgula: "62,7%". */
fun formatShare(share: Double): String {
    val tenths = (share * 10).roundToLong()
    return "${tenths / 10},${tenths % 10}%"
}

/**
 * Frase do lugar, como a `placeSentence` do site: "No Brasil, Flavio Bolsonaro teve 56.104.503 votos
 * e Lula 53.879.538. Diferença de 2,2 milhões de votos. 100% das seções apuradas."
 */
fun placeSentence(place: PlaceResult): String {
    val leaderVotes = place.leader?.votes ?: 0
    val runner = place.runnerUp
    val runnerVotes = runner?.votes ?: 0
    val leaderName = place.leader?.ballotName.orEmpty().toNameProse()
    val votesPart = if (runner != null) {
        "$leaderName teve ${formatVotes(leaderVotes)} votos e ${runner.ballotName.orEmpty().toNameProse()} " +
            "${formatVotes(runnerVotes)}."
    } else {
        // Sem segundo colocado, ele conta como 0 votos e a frase fecha no líder.
        "$leaderName teve ${formatVotes(leaderVotes)} votos."
    }
    val counted = place.countedShare?.let { " ${jsRound(it)}% das seções apuradas." }.orEmpty()
    val decided = if (
        place.office == Office.GOVERNOR && place.scope == PlaceScope.UF && (place.leader?.voteShare ?: 0.0) > 50.0
    ) {
        " A eleição foi decidida no primeiro turno."
    } else {
        ""
    }
    return "${placePrefix(place)}, $votesPart ${marginText(abs(leaderVotes - runnerVotes))}$counted$decided"
}

private fun placePrefix(place: PlaceResult): String {
    if (place.scope == PlaceScope.BRASIL) return "No Brasil"
    if (place.code == EXTERIOR || place.state == EXTERIOR) return "No exterior"
    if (place.scope == PlaceScope.MUNICIPIO) return "No município de ${place.name.orEmpty().toNameProse()}"
    val uf = Ufs.byCode(place.state)
    return if (uf != null) "${uf.preposition} ${uf.name}" else "Em ${place.name.orEmpty().toNameProse()}"
}

private fun marginText(diff: Long): String {
    if (diff < 1_000_000) return "Diferença de ${formatVotes(diff)} votos."
    val tenths = jsRound(diff / 100_000.0)
    val number = if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10},${tenths % 10}"
    val unit = if (tenths == 10L) "milhão" else "milhões"
    return "Diferença de $number $unit de votos."
}

/** Mesmo arredondamento do Math.round do JavaScript. */
private fun jsRound(value: Double): Long = floor(value + 0.5).toLong()
