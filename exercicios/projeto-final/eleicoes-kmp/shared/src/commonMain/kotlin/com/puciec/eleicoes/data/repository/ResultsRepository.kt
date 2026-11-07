package com.puciec.eleicoes.data.repository

import com.puciec.eleicoes.data.mapper.toDomain
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceResult
import com.puciec.eleicoes.domain.model.PlaceScope
import kotlin.time.Duration.Companion.minutes

/**
 * Resultados por local, usados pelo Mapa (#6). Só há dados do 1º turno:
 * `round = 2` volta vazio. Cargos aceitos: presidente, governador, senador e deputados.
 */
class ResultsRepository(private val source: EleicoesDataSource) {
    private val cache = MemoryCache(ttl = 10.minutes)

    /** Todos os locais de uma abrangência: o Brasil, as 27 UFs mais o exterior ("ZZ") ou os municípios de uma UF. */
    suspend fun places(
        office: Office,
        scope: PlaceScope,
        state: String? = null,
        round: Int? = FIRST_ROUND,
    ): List<PlaceResult> = cache.getOrLoad("places|$office|$scope|$state|$round") {
        source.listPlaceResults(office, scope, state, round).map { it.toDomain() }
    }

    /** Um local, com os votos de cada candidato. [code] é a UF ("SP"), "BR", "ZZ" ou o código do município. */
    suspend fun place(
        code: String,
        office: Office,
        scope: PlaceScope,
        round: Int? = FIRST_ROUND,
    ): PlaceResult = cache.getOrLoad("place|$code|$office|$scope|$round") {
        source.getPlaceResult(code, office, scope, round).toDomain()
    }

    companion object {
        const val FIRST_ROUND = 1
    }
}
