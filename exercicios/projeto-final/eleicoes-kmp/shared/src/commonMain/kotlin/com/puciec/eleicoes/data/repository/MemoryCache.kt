package com.puciec.eleicoes.data.repository

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cache em memória com validade. Evita repetir a mesma consulta ao trocar de aba
 * e ajuda a respeitar o limite da API (60 req/min por chave). Some ao fechar o app;
 * o cache em disco é o #12.
 */
internal class MemoryCache(private val ttl: Duration) {
    private class Entry(val value: Any?, val storedAt: TimeSource.Monotonic.ValueTimeMark)

    private val entries = mutableMapOf<String, Entry>()
    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> getOrLoad(key: String, load: suspend () -> T): T {
        mutex.withLock {
            val entry = entries[key]
            if (entry != null && entry.storedAt.elapsedNow() < ttl) return entry.value as T
        }
        val value = load()
        mutex.withLock { entries[key] = Entry(value, TimeSource.Monotonic.markNow()) }
        return value
    }

    suspend fun clear() = mutex.withLock { entries.clear() }
}
