package com.puciec.eleicoes.android.ui.state

import com.puciec.eleicoes.domain.model.AppError
import kotlin.coroutines.cancellation.CancellationException

/** Estado de uma tela que carrega dados: o enunciado pede carregando, vazio e erro, nunca tela branca. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<out T>(val data: T) : UiState<T>

    data class Empty(val message: String? = null) : UiState<Nothing>

    data class Error(val error: AppError) : UiState<Nothing>
}

fun Throwable.toAppError(): AppError = this as? AppError ?: AppError.Unexpected(message)

/**
 * Roda [block] e devolve Success, Empty (lista vazia) ou Error. Use dentro do viewModelScope:
 * `_state.value = loadState { repository.algo() }`.
 */
suspend fun <T> loadState(
    isEmpty: (T) -> Boolean = { (it as? Collection<*>)?.isEmpty() == true },
    block: suspend () -> T,
): UiState<T> =
    try {
        val data = block()
        if (isEmpty(data)) UiState.Empty() else UiState.Success(data)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        UiState.Error(e.toAppError())
    }

/** Texto para o usuário. Nunca mostre a exceção crua. */
fun AppError.userMessage(): String = when (this) {
    AppError.MissingApiKey -> "Falta a chave da API. Veja o README para configurar o ELEICOES_API_KEY."
    AppError.InvalidApiKey -> "A API recusou a chave. Confira o ELEICOES_API_KEY no .env e builde de novo."
    AppError.NoConnection -> "Sem conexão. Verifique a internet e tente de novo."
    is AppError.RateLimited -> "Muitas consultas em pouco tempo. Tente de novo em ${retryAfterSeconds ?: 60} s."
    AppError.NotFound -> "Não encontramos esse item."
    is AppError.Server -> "A API eleicoes.dev está com problema agora. Tente de novo em instantes."
    is AppError.Unexpected -> "Os dados vieram num formato inesperado."
}
