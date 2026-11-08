package com.puciec.eleicoes.domain.partidos

/**
 * Quando os dados da aba Partidos foram buscados na API, para o usuário saber se vale recarregar:
 * "Atualizado agora", "há 5 min", "há 3 h", "há 2 dias". Relógio do aparelho no passado conta como agora.
 */
fun updatedAgo(savedAtEpochMillis: Long, nowEpochMillis: Long): String {
    val minutes = ((nowEpochMillis - savedAtEpochMillis) / 60_000).coerceAtLeast(0)
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Atualizado agora"
        hours < 1 -> "Atualizado há $minutes min"
        days < 1 -> "Atualizado há $hours h"
        days == 1L -> "Atualizado há 1 dia"
        else -> "Atualizado há $days dias"
    }
}
