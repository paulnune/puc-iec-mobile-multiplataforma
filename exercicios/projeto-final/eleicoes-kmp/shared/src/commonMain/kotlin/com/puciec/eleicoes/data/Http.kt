package com.puciec.eleicoes.data

import com.puciec.eleicoes.domain.model.AppError
import io.ktor.client.HttpClient
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Cada plataforma escolhe o seu motor HTTP (expect/actual, como na aula 5). */
internal expect fun createHttpClient(): HttpClient

/** Json tolerante: ignora campos novos da API e aceita nulos onde o DTO tem valor padrão. */
val EleicoesJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

/** Executa uma chamada de rede e converte falha de conexão em [AppError.NoConnection]. */
internal suspend fun <T> networkCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AppError) {
        throw e
    } catch (e: Exception) {
        throw AppError.NoConnection
    }

/** Converte o status HTTP no erro que a UI entende. */
internal fun checkHttpStatus(status: Int, retryAfter: String?) {
    when {
        status == 401 || status == 403 -> throw AppError.InvalidApiKey
        status == 404 -> throw AppError.NotFound
        status == 429 -> throw AppError.RateLimited(retryAfter?.trim()?.toIntOrNull())
        status >= 500 -> throw AppError.Server("HTTP $status")
        status !in 200..299 -> throw AppError.Unexpected("HTTP $status")
    }
}

internal fun <T> decodeOrThrow(json: Json, serializer: KSerializer<T>, text: String, what: String): T =
    try {
        json.decodeFromString(serializer, text)
    } catch (e: IllegalArgumentException) {
        // SerializationException herda de IllegalArgumentException.
        throw AppError.Unexpected("Formato inesperado em $what")
    }
