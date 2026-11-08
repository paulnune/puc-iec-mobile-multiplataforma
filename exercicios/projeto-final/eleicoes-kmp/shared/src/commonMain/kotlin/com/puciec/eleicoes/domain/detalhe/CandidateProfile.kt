package com.puciec.eleicoes.domain.detalhe

import com.puciec.eleicoes.domain.format.toTitleCase
import com.puciec.eleicoes.domain.model.Candidate

/** Data de referência para a idade: 1º turno de 2026 (04/10/2026). */
object ElectionDay2026 {
    const val YEAR = 2026
    const val MONTH = 10
    const val DAY = 4
}

/**
 * Idade completa em anos na data de referência. A API manda `yyyy-MM-dd` (com ou sem hora depois);
 * `dd/MM/yyyy` é aceito por tolerância. Data ausente, inválida ou no futuro devolve nulo.
 */
fun candidateAge(
    birthDate: String?,
    onYear: Int = ElectionDay2026.YEAR,
    onMonth: Int = ElectionDay2026.MONTH,
    onDay: Int = ElectionDay2026.DAY,
): Int? {
    val (year, month, day) = parseDate(birthDate) ?: return null
    var age = onYear - year
    if (onMonth < month || (onMonth == month && onDay < day)) age--
    return age.takeIf { it >= 0 }
}

/** "69 anos", ou nulo sem data de nascimento. */
fun Candidate.ageLabel(): String? = candidateAge(birthDate)?.let { if (it == 1) "1 ano" else "$it anos" }

/**
 * Naturalidade para o perfil. Naturalizados: "Naturalizado(a) · nascido(a) em <Cidade>";
 * os demais: "<Cidade> (UF)". A UF "ZZ" (exterior) não é exibida.
 */
fun Candidate.birthplaceLabel(): String? {
    val city = birthCity?.trim()?.ifBlank { null }?.toTitleCase()
    if (isNaturalized) {
        return if (city != null) "Naturalizado(a) · nascido(a) em $city" else "Naturalizado(a)"
    }
    val uf = birthState?.trim()?.uppercase()?.takeIf { it.isNotBlank() && it != FOREIGN_UF }
    return when {
        city != null && uf != null -> "$city ($uf)"
        city != null -> city
        uf != null -> uf
        else -> null
    }
}

private const val FOREIGN_UF = "ZZ"

private val ISO_DATE = Regex("""(\d{4})-(\d{2})-(\d{2})""")
private val BR_DATE = Regex("""(\d{2})/(\d{2})/(\d{4})""")

private fun parseDate(raw: String?): Triple<Int, Int, Int>? {
    val text = raw?.trim()?.take(10) ?: return null
    val date = ISO_DATE.matchEntire(text)?.destructured?.let { (y, m, d) -> Triple(y, m, d) }
        ?: BR_DATE.matchEntire(text)?.destructured?.let { (d, m, y) -> Triple(y, m, d) }
        ?: return null
    val year = date.first.toInt()
    val month = date.second.toInt()
    val day = date.third.toInt()
    if (month !in 1..12 || day !in 1..31) return null
    return Triple(year, month, day)
}
