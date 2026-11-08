package com.puciec.eleicoes.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

/** No iOS o motor HTTP é o da Apple (NSURLSession), com as mesmas regras do Android. */
internal actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    // Os status de erro são tratados à mão (checkHttpStatus), para virar AppError.
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}
