package com.puciec.eleicoes.android.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "tema")

/** Grava a escolha de tema no aparelho: sobrevive a fechar o app. */
class ThemeStore(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.themeDataStore)

    val preference: Flow<ThemePreference> =
        dataStore.data.map { prefs -> ThemePreference.fromStorage(prefs[KEY_THEME]) }

    suspend fun save(preference: ThemePreference) {
        dataStore.edit { prefs -> prefs[KEY_THEME] = preference.name }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("preferencia_tema")
    }
}
