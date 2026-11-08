package com.puciec.eleicoes.ui.feature.colinha

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.puciec.eleicoes.domain.favorites.FavoriteCandidate
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Favoritos gravados no aparelho (#5): fechar e reabrir o app mantém a colinha.
 * A lista vai como JSON numa chave só do DataStore, na ordem em que foi favoritada.
 */
class DataStoreFavoritesRepository(private val dataStore: DataStore<Preferences>) : FavoritesRepository {

    override val favorites: Flow<List<FavoriteCandidate>> =
        dataStore.data
            .map { prefs -> decode(prefs[KEY_FAVORITES]) }
            .distinctUntilChanged()

    override suspend fun add(candidate: FavoriteCandidate) = edit { current ->
        current.filterNot { it.id == candidate.id } + candidate
    }

    override suspend fun remove(candidateId: String) = edit { current ->
        current.filterNot { it.id == candidateId }
    }

    private suspend fun edit(transform: (List<FavoriteCandidate>) -> List<FavoriteCandidate>) {
        dataStore.edit { prefs -> prefs[KEY_FAVORITES] = encode(transform(decode(prefs[KEY_FAVORITES]))) }
    }

    private companion object {
        val KEY_FAVORITES = stringPreferencesKey("favoritos")
        val serializer = ListSerializer(FavoriteCandidate.serializer())
        val json = Json { ignoreUnknownKeys = true }

        fun encode(list: List<FavoriteCandidate>): String = json.encodeToString(serializer, list)

        /** JSON ausente ou de uma versão antiga incompatível vira colinha vazia, em vez de quebrar o app. */
        fun decode(raw: String?): List<FavoriteCandidate> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                json.decodeFromString(serializer, raw)
            } catch (e: SerializationException) {
                emptyList()
            } catch (e: IllegalArgumentException) {
                emptyList()
            }
        }
    }
}
