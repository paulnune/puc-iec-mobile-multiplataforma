package com.puciec.eleicoes.data.repository

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cache em memória com validade. Evita repetir a mesma consulta ao trocar de aba
 * e ajuda a respeitar o limite da API (60 req/min por chave). Some ao fechar o app;
 * o cache em disco é o #12.
 *
 * Chamadas simultâneas com a mesma chave dividem uma só consulta: as outras esperam o resultado.
 * Se quem carregava for cancelado, as que esperavam recarregam por conta própria. Se a consulta
 * falha, todas que esperavam recebem a mesma falha e a próxima chamada tenta de novo.
 */
internal class MemoryCache(private val ttl: Duration) {
    private class Entry(val value: Any?, val storedAt: TimeSource.Monotonic.ValueTimeMark)

    private val entries = mutableMapOf<String, Entry>()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Any?>>()
    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> getOrLoad(key: String, load: suspend () -> T): T {
        while (true) {
            var owner = false
            var pending: CompletableDeferred<Any?>? = null
            val cached: Entry? = mutex.withLock {
                entries[key]?.takeIf { it.storedAt.elapsedNow() < ttl } ?: run {
                    pending = inFlight[key]
                    if (pending == null) {
                        pending = CompletableDeferred()
                        inFlight[key] = pending!!
                        owner = true
                    }
                    null
                }
            }
            if (cached != null) return cached.value as T
            if (owner) return loadAndPublish(key, pending!!, load)
            try {
                return pending!!.await() as T
            } catch (e: CancellationException) {
                // Cancelamento de quem esperava: sai. Se foi de quem carregava, tenta de novo.
                currentCoroutineContext().ensureActive()
            }
        }
    }

    private suspend fun <T> loadAndPublish(key: String, pending: CompletableDeferred<Any?>, load: suspend () -> T): T {
        try {
            val value = load()
            mutex.withLock { entries[key] = Entry(value, TimeSource.Monotonic.markNow()) }
            pending.complete(value)
            return value
        } catch (e: Throwable) {
            pending.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }

    suspend fun clear() = mutex.withLock {
        entries.clear()
    }
}
