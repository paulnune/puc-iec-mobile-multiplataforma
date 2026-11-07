package com.puciec.eleicoes.domain.format

private val LOWERCASE_WORDS = setOf("de", "da", "do", "das", "dos", "e")

/**
 * A API devolve nomes e cargos em maiúsculas ("DRA. GEORGIA MICHELETTI", "1º SUPLENTE").
 * Para exibir: "Dra. Georgia Micheletti", "1º Suplente".
 */
fun String.toTitleCase(): String =
    lowercase()
        .split(" ")
        .filter { it.isNotEmpty() }
        .mapIndexed { index, word ->
            if (index > 0 && word in LOWERCASE_WORDS) word else word.replaceFirstChar { it.uppercaseChar() }
        }
        .joinToString(" ")
