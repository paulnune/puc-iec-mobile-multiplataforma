package com.puciec.eleicoes.data.repository

import com.puciec.eleicoes.data.mapper.toDomain
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.model.Party
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes

/** Partidos de 2026 (38 na API). Carrega uma vez e guarda por 30 minutos. */
class PartiesRepository(private val source: EleicoesDataSource) {
    private val cache = MemoryCache(ttl = 30.minutes)

    /** Todos os partidos, ordenados pelo número. */
    suspend fun all(): List<Party> = cache.getOrLoad("all") {
        val first = source.listParties(query = null, page = 1, limit = PAGE_SIZE)
        val others = (2..(first.pagination?.totalPages ?: 1)).flatMap { page ->
            source.listParties(query = null, page = page, limit = PAGE_SIZE).data
        }
        (first.data + others).map { it.toDomain() }.sortedBy { it.number }
    }

    suspend fun party(id: String): Party =
        all().firstOrNull { it.id == id } ?: cache.getOrLoad("party|$id") { source.getParty(id).toDomain() }

    /**
     * Sigla por id do partido, para os cards ("PL", "PT"...). Se a lista de partidos falhar,
     * os candidatos aparecem sem a sigla em vez de a tela inteira dar erro.
     */
    suspend fun acronymsById(): Map<String, String> =
        try {
            all().associate { it.id to it.acronym }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
