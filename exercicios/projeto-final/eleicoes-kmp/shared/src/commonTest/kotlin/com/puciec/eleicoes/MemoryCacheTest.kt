package com.puciec.eleicoes

import com.puciec.eleicoes.data.repository.MemoryCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** Cache em memória: chamadas simultâneas com a mesma chave dividem uma só consulta (#7). */
class MemoryCacheTest {

    @Test
    fun `chamadas simultaneas com a mesma chave fazem uma so consulta`() = runTest {
        val cache = MemoryCache(ttl = 5.minutes)
        val gate = CompletableDeferred<Unit>()
        var loads = 0

        val first = async { cache.getOrLoad("k") { loads++; gate.await(); "valor" } }
        val second = async { cache.getOrLoad("k") { loads++; "outro" } }
        runCurrent()
        gate.complete(Unit)

        assertEquals("valor", first.await())
        assertEquals("valor", second.await())
        assertEquals(1, loads)
    }

    @Test
    fun `se quem carregava for cancelado a outra chamada carrega de novo`() = runTest {
        val cache = MemoryCache(ttl = 5.minutes)
        val owner = launch { cache.getOrLoad("k") { awaitCancellation() } }
        runCurrent()
        val waiter = async { cache.getOrLoad("k") { "recarregado" } }
        runCurrent()

        owner.cancelAndJoin()

        assertEquals("recarregado", waiter.await())
    }

    @Test
    fun `falha vale para quem esperava e a proxima chamada tenta de novo`() = runTest {
        val cache = MemoryCache(ttl = 5.minutes)
        val gate = CompletableDeferred<Unit>()

        val first = async { runCatching { cache.getOrLoad("k") { gate.await(); error("falha") } } }
        val second = async { runCatching { cache.getOrLoad("k") { "nao usado" } } }
        runCurrent()
        gate.complete(Unit)

        assertTrue(first.await().isFailure)
        assertTrue(second.await().isFailure)
        assertEquals("ok", cache.getOrLoad("k") { "ok" })
    }
}
