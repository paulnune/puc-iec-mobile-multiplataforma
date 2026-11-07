package com.puciec.eleicoes.domain.model

/**
 * Erros que a camada de dados entrega à UI. Cada tela decide o texto
 * (androidApp: ui/state/ErrorMessages.kt), nunca mostra a exceção crua.
 */
sealed class AppError(message: String) : Exception(message) {
    data object MissingApiKey : AppError("Chave da API não configurada")

    data object InvalidApiKey : AppError("Chave da API recusada")

    data object NoConnection : AppError("Sem conexão")

    data class RateLimited(val retryAfterSeconds: Int?) : AppError("Limite de requisições atingido")

    data object NotFound : AppError("Não encontrado")

    data class Server(val detail: String?) : AppError("Erro no servidor da API")

    data class Unexpected(val detail: String?) : AppError("Resposta inesperada")
}
