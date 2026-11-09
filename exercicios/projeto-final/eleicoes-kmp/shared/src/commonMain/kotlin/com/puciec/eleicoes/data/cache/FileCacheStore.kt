package com.puciec.eleicoes.data.cache

import com.puciec.eleicoes.data.EleicoesJson
import com.puciec.eleicoes.platform.sha256Hex
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cache offline (#12) em arquivos: uma resposta por consulta em [directoryPath] (no app, uma subpasta
 * de [com.puciec.eleicoes.platform.appFilesDirPath], que o sistema não limpa sozinho como o cache).
 *
 * O nome do arquivo é o SHA-256 da chave. A gravação vai para um arquivo temporário e depois troca
 * de nome (move atômico), então nunca fica um arquivo pela metade.
 */
class FileCacheStore(
    private val directoryPath: String,
    private val json: Json = EleicoesJson,
) : CacheStore {

    @Serializable
    private data class Envelope(val savedAt: Long, val json: String)

    override suspend fun read(key: String): CachedResponse? = withContext(Dispatchers.Default) {
        val file = fileFor(key)
        if (!SystemFileSystem.exists(file)) return@withContext null
        try {
            val text = SystemFileSystem.source(file).buffered().use { it.readString() }
            val envelope = json.decodeFromString(Envelope.serializer(), text)
            CachedResponse(json = envelope.json, savedAtEpochMillis = envelope.savedAt)
        } catch (e: IllegalArgumentException) {
            null // arquivo corrompido (SerializationException herda de IllegalArgumentException)
        } catch (e: IOException) {
            null
        }
    }

    override suspend fun write(key: String, response: CachedResponse) {
        withContext(Dispatchers.Default) {
            val directory = Path(directoryPath)
            SystemFileSystem.createDirectories(directory)
            val target = fileFor(key)
            val temp = Path(directory, "${target.name}.${Random.nextLong().toULong().toString(16)}.tmp")
            try {
                val envelope = Envelope(response.savedAtEpochMillis, response.json)
                SystemFileSystem.sink(temp).buffered().use { it.writeString(json.encodeToString(Envelope.serializer(), envelope)) }
                SystemFileSystem.atomicMove(temp, target)
            } finally {
                SystemFileSystem.delete(temp, mustExist = false)
            }
        }
    }

    private fun fileFor(key: String): Path = Path(directoryPath, "${sha256Hex(key)}.json")
}
