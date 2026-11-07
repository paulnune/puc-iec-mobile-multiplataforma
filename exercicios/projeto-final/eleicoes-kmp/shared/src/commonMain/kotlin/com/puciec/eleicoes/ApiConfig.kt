package com.puciec.eleicoes

/**
 * Configuração da API eleicoes.dev. A chave entra em tempo de build, lida do `.env`
 * (ver shared/build.gradle.kts), e nunca vai para o git.
 */
object ApiConfig {
    const val MCP_ENDPOINT = "https://eleicoes.dev/mcp"
    const val REST_BASE_URL = "https://eleicoes.dev"

    val apiKey: String get() = BuildKonfig.ELEICOES_API_KEY

    val hasApiKey: Boolean get() = apiKey.isNotBlank()
}
