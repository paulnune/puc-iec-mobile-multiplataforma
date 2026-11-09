package com.puciec.eleicoes

import com.puciec.eleicoes.data.cache.CachedResponse
import com.puciec.eleicoes.data.cache.FileCacheStore
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

/** Cache offline (#12): o armazenamento em arquivo, numa pasta temporária. */
class FileCacheStoreTest {
    private val directory: File = Files.createTempDirectory("eleicoes-cache").toFile()
    private val store = FileCacheStore(directory.absolutePath)

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun `grava e le a mesma resposta`() = runTest {
        val response = CachedResponse(json = """[{"code":"SP","name":"SÃO PAULO"}]""", savedAtEpochMillis = 1_762_500_000_000)
        store.write("listPlaceResults|president|uf||1", response)

        assertEquals(response, store.read("listPlaceResults|president|uf||1"))
        // Uma instância nova (app reaberto) lê o mesmo arquivo.
        assertEquals(response, FileCacheStore(directory.absolutePath).read("listPlaceResults|president|uf||1"))
    }

    @Test
    fun `chave ausente devolve nulo`() = runTest {
        store.write("getParty|p13", CachedResponse(json = "{}", savedAtEpochMillis = 1))
        assertNull(store.read("getParty|p22"))
        assertNull(FileCacheStore(File(directory, "pasta-que-nao-existe").absolutePath).read("getParty|p13"))
    }

    @Test
    fun `arquivo corrompido devolve nulo`() = runTest {
        store.write("getParty|p13", CachedResponse(json = "{}", savedAtEpochMillis = 1))
        val file = directory.listFiles()!!.single()
        file.writeText("{metade de um arqu")

        assertNull(store.read("getParty|p13"))
    }

    @Test
    fun `gravacoes simultaneas deixam um arquivo inteiro`() = runTest {
        val payloads = (1..40).map { i -> "\"resposta-$i-${"x".repeat(200_000)}\"" }
        coroutineScope {
            payloads.forEachIndexed { i, payload ->
                launch(Dispatchers.IO) { store.write("listCandidates|;;;;;|1|30", CachedResponse(payload, i.toLong())) }
            }
        }

        val read = assertNotNull(store.read("listCandidates|;;;;;|1|30"))
        assertTrue(read.json in payloads, "o arquivo tem uma das respostas inteira")
        assertEquals(payloads.indexOf(read.json).toLong(), read.savedAtEpochMillis, "resposta e hora da mesma gravação")
        assertEquals(1, directory.listFiles()!!.size, "nenhum arquivo temporário sobrou")
    }
}
