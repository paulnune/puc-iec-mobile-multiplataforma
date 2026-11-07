package com.puciec.eleicoes.domain.filter

import com.puciec.eleicoes.domain.model.Candidate

/**
 * Busca da feature 3: filtra no aparelho a lista já carregada, sem chamar a API.
 *
 * Compara sem diferenciar maiúsculas nem acentos ("joao" acha "JOÃO") e procura tanto no
 * nome de urna quanto no nome completo civil.
 */
fun List<Candidate>.matchingText(text: String): List<Candidate> {
    val query = text.normalizedForSearch()
    if (query.isEmpty()) return this
    return filter {
        it.ballotName.normalizedForSearch().contains(query) ||
            it.fullName.normalizedForSearch().contains(query)
    }
}

/** Minúsculas + troca de letras acentuadas pela equivalente sem acento. */
private fun String.normalizedForSearch(): String =
    trim()
        .lowercase()
        .map { char -> DIACRITICS[char] ?: char }
        .joinToString(separator = "")

private val DIACRITICS: Map<Char, Char> = mapOf(
    'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a', 'å' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    'ç' to 'c',
    'ñ' to 'n',
    'ý' to 'y', 'ÿ' to 'y',
)
