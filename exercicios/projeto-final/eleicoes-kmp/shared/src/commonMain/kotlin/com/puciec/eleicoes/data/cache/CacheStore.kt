package com.puciec.eleicoes.data.cache

/** Uma resposta da API guardada como veio ([json]) e o instante em que foi salva. */
data class CachedResponse(val json: String, val savedAtEpochMillis: Long)

/**
 * Porta do armazenamento do cache offline (#12). O decorator [CachingEleicoesDataSource] decide
 * quando ler e gravar; quem implementa só guarda e devolve. No app, o `FileCacheStore` grava um
 * arquivo por consulta; nos testes, um mapa em memória.
 */
interface CacheStore {
    /** A resposta salva para [key], ou nulo se não houver (ou se não der para ler). */
    suspend fun read(key: String): CachedResponse?

    suspend fun write(key: String, response: CachedResponse)
}
