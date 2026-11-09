package com.puciec.eleicoes.data

import com.puciec.eleicoes.data.cache.CachedResponse
import com.puciec.eleicoes.data.cache.FileCacheStore
import com.puciec.eleicoes.platform.sha256Hex
import com.puciec.eleicoes.testTempDirPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString

class FileCacheStoreMultiplatformTest {
    private fun newStore(): Pair<FileCacheStore, String> {
        val dir = testTempDirPath() + "/cache-" + sha256Hex(kotlin.random.Random.nextLong().toString()).take(12)
        return FileCacheStore(dir) to dir
    }

    @Test
    fun readReturnsWhatWasWritten() = runTest {
        val (store, _) = newStore()
        val saved = CachedResponse(json = """{"a":1}""", savedAtEpochMillis = 1234L)
        store.write("consulta", saved)
        assertEquals(saved, store.read("consulta"))
    }

    @Test
    fun readReturnsNullForUnknownKey() = runTest {
        val (store, _) = newStore()
        assertNull(store.read("nunca-gravada"))
    }

    @Test
    fun readReturnsNullForCorruptedFile() = runTest {
        val (store, dir) = newStore()
        store.write("k", CachedResponse("{}", 1L))
        SystemFileSystem.sink(Path(dir, sha256Hex("k") + ".json")).buffered().use { it.writeString("não é json") }
        assertNull(store.read("k"))
    }

    @Test
    fun differentKeysDoNotCollide() = runTest {
        val (store, _) = newStore()
        store.write("a", CachedResponse("1", 1L))
        store.write("b", CachedResponse("2", 2L))
        assertNotEquals(store.read("a"), store.read("b"))
        assertEquals("1", store.read("a")?.json)
    }
}
