package com.puciec.eleicoes.data.cache

import com.puciec.eleicoes.data.EleicoesJson
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cache offline (#12) em arquivos: uma resposta por consulta em [directory] (no app,
 * `filesDir/eleicoes-cache`, que o sistema não limpa sozinho como o `cacheDir`).
 *
 * O nome do arquivo é o SHA-256 da chave. A gravação vai para um arquivo temporário e depois troca
 * de nome (rename atômico no Android), então nunca fica um arquivo pela metade.
 */
class FileCacheStore(
    private val directory: File,
    private val json: Json = EleicoesJson,
) : CacheStore {

    @Serializable
    private data class Envelope(val savedAt: Long, val json: String)

    override suspend fun read(key: String): CachedResponse? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (!file.isFile) return@withContext null
        try {
            val envelope = json.decodeFromString(Envelope.serializer(), file.readText())
            CachedResponse(json = envelope.json, savedAtEpochMillis = envelope.savedAt)
        } catch (e: IllegalArgumentException) {
            null // arquivo corrompido (SerializationException herda de IllegalArgumentException)
        } catch (e: IOException) {
            null
        }
    }

    override suspend fun write(key: String, response: CachedResponse) {
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            val target = fileFor(key)
            val temp = File.createTempFile(target.nameWithoutExtension, ".tmp", directory)
            try {
                temp.writeText(json.encodeToString(Envelope.serializer(), Envelope(response.savedAtEpochMillis, response.json)))
                if (!temp.renameTo(target)) throw IOException("Não deu para gravar ${target.name}")
            } finally {
                temp.delete()
            }
        }
    }

    private fun fileFor(key: String): File = File(directory, "${sha256(key)}.json")

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.encodeToByteArray())
            .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
