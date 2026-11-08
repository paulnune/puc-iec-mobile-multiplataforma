package com.puciec.eleicoes.domain.partidos

import com.puciec.eleicoes.domain.format.toTitleCase
import com.puciec.eleicoes.domain.model.Party

/** Ordem da lista de partidos (#8). */
enum class PartySort(val label: String) {
    /** Número da urna (10, 11, 12…): a ordem em que a API devolve. */
    NUMBER("Número"),

    /** Sigla de A a Z; no empate, pelo número. */
    ACRONYM("Sigla"),
}

/**
 * Nome para exibir abaixo da sigla, em title case ("Partido da Social Democracia Brasileira").
 * Quando a API manda o nome igual à sigla (REPUBLICANOS, CIDADANIA, DEMOCRATA, AGIR, AVANTE),
 * o nome aparece mesmo assim ("Republicanos"): todas as linhas ficam no formato sigla + nome.
 * Nulo só se o nome vier em branco.
 */
fun Party.displayName(): String? = name.takeIf { it.isNotBlank() }?.toTitleCase()

/**
 * Chave de um registro conhecido da API: número da urna **e** sigla como a API manda hoje. O TSE reaproveita
 * números de partidos extintos; exigir os dois evita esconder ou renomear um partido novo que herde o número.
 */
private data class KnownParty(val number: Int, val apiAcronym: String)

private fun Party.key() = KnownParty(number, acronym.trim())

/**
 * Partidos que a tool `list_parties` devolve mas que não têm candidaturas em 2026 (conferido com
 * list_candidates em 07/11: 0 cada). São de eleições anteriores e ficam na API para a linha do tempo (#7).
 */
private val PARTIES_WITHOUT_2026_CANDIDATES = setOf(
    KnownParty(17, "PSL"),
    KnownParty(19, "PTN"),
    KnownParty(26, "PAN"),
    KnownParty(31, "PHS"),
    KnownParty(51, "PATRIOTA"),
    KnownParty(54, "PPL"),
    KnownParty(56, "PRONA"),
    KnownParty(90, "PROS"),
)

/** Sigla oficial quando a API manda outra grafia: "PC do B" → "PCdoB", "SD" → "SOLIDARIEDADE". */
private val OFFICIAL_ACRONYMS = mapOf(
    KnownParty(65, "PC do B") to "PCdoB",
    KnownParty(77, "SD") to "SOLIDARIEDADE",
)

/** Nome oficial quando a API perde os acentos ("PARTIDO RENOVACAO DEMOCRATICA"). */
private val OFFICIAL_NAMES = mapOf(
    KnownParty(25, "PRD") to "PARTIDO RENOVAÇÃO DEMOCRÁTICA",
)

/**
 * Lista da aba Partidos com sigla e nome oficiais: hoje são os 30 da tabela do grupo (AGIR, AVANTE,
 * CIDADANIA, DC… UP).
 *
 * Resiliente a mudanças futuras na API: só os registros conhecidos (número + sigla) são escondidos ou
 * corrigidos. Um partido novo, inclusive um que reaproveite o número de um extinto, aparece exatamente como
 * a API mandou. Os ids continuam os da API, então os filtros por partido não mudam.
 */
fun List<Party>.officialParties(): List<Party> =
    filterNot { it.key() in PARTIES_WITHOUT_2026_CANDIDATES }.map { party ->
        party.copy(
            acronym = OFFICIAL_ACRONYMS[party.key()] ?: party.acronym,
            name = OFFICIAL_NAMES[party.key()] ?: party.name,
        )
    }

/**
 * Busca local nos partidos (sem chamar a API): pela sigla, pelo nome ou pelo número,
 * sem diferenciar maiúsculas nem acentos ("democratico" acha "DEMOCRÁTICO"). Busca vazia devolve todos.
 */
fun List<Party>.filterAndSort(query: String, sort: PartySort): List<Party> {
    val q = query.normalizedForSearch()
    val filtered = if (q.isEmpty()) this else filter { it.matches(q) }
    return when (sort) {
        PartySort.NUMBER -> filtered.sortedBy { it.number }
        PartySort.ACRONYM -> filtered.sortedWith(
            compareBy<Party> { it.acronym.normalizedForSearch() }.thenBy { it.number },
        )
    }
}

private fun Party.matches(q: String): Boolean =
    number.toString() == q ||
        acronym.normalizedForSearch().contains(q) ||
        name.normalizedForSearch().contains(q)

private val ACCENTS = mapOf(
    'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    'ç' to 'c', 'ñ' to 'n',
)

/** Minúsculas, sem acento e com espaços simples (a API manda "SOCIALISTA  DOS"). */
private fun String.normalizedForSearch(): String =
    lowercase()
        .map { ACCENTS[it] ?: it }
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }
        .joinToString(" ")
