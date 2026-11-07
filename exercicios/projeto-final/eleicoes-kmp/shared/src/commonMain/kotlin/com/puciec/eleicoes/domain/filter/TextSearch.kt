package com.puciec.eleicoes.domain.filter

import com.puciec.eleicoes.domain.model.Candidate

/**
 * Busca da feature 3: filtra no aparelho a lista já carregada, sem chamar a API.
 *
 * TODO(#4): ignorar acentos ("joao" acha "JOÃO") e procurar também no nome completo,
 * com testes em shared/src/commonTest.
 */
fun List<Candidate>.matchingText(text: String): List<Candidate> {
    val query = text.trim()
    if (query.isEmpty()) return this
    return filter { it.ballotName.contains(query, ignoreCase = true) }
}
