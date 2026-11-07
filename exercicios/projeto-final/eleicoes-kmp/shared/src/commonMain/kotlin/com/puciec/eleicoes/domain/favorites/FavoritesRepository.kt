package com.puciec.eleicoes.domain.favorites

import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

/**
 * Retrato do candidato guardado na colinha. Tem o necessário para mostrar a cola sem rede.
 * É @Serializable para a implementação persistente (#5) gravar como JSON.
 */
@Serializable
data class FavoriteCandidate(
    val id: String,
    val ballotName: String,
    val number: Int,
    val office: Office,
    val officeLabel: String,
    val state: String,
    val partyAcronym: String?,
    val photoUrl: String?,
    val outcome: Outcome,
)

fun Candidate.toFavorite(): FavoriteCandidate = FavoriteCandidate(
    id = id,
    ballotName = ballotName,
    number = number,
    office = office,
    officeLabel = officeLabel,
    state = state,
    partyAcronym = partyAcronym,
    photoUrl = photoUrl,
    outcome = outcome,
)

/** Contrato dos favoritos. O Detalhe (#3) usa; a Colinha (#5) entrega a versão persistente. */
interface FavoritesRepository {
    val favorites: Flow<List<FavoriteCandidate>>

    suspend fun add(candidate: FavoriteCandidate)

    suspend fun remove(candidateId: String)
}

fun FavoritesRepository.isFavorite(candidateId: String): Flow<Boolean> =
    favorites.map { list -> list.any { it.id == candidateId } }

suspend fun FavoritesRepository.toggle(candidate: FavoriteCandidate) {
    if (favorites.first().any { it.id == candidate.id }) remove(candidate.id) else add(candidate)
}

/** Implementação provisória: some ao fechar o app. O #5 troca no AppContainer. */
class InMemoryFavoritesRepository : FavoritesRepository {
    private val state = MutableStateFlow<List<FavoriteCandidate>>(emptyList())

    override val favorites: Flow<List<FavoriteCandidate>> = state.asStateFlow()

    override suspend fun add(candidate: FavoriteCandidate) {
        state.update { current -> current.filterNot { it.id == candidate.id } + candidate }
    }

    override suspend fun remove(candidateId: String) {
        state.update { current -> current.filterNot { it.id == candidateId } }
    }
}
