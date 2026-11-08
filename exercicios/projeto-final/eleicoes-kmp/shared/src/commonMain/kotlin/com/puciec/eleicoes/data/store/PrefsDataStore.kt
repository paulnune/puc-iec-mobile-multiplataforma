package com.puciec.eleicoes.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.puciec.eleicoes.platform.appFilesDirPath
import okio.Path.Companion.toPath

private val stores = mutableMapOf<String, DataStore<Preferences>>()

/**
 * DataStore de preferências em `appFilesDirPath()/<fileName>`. Devolve sempre a mesma instância para o
 * mesmo [fileName]: dois DataStores no mesmo arquivo derrubam o app com IllegalStateException.
 * Chamar da thread principal, na criação do container.
 */
fun createPrefsDataStore(fileName: String): DataStore<Preferences> =
    stores.getOrPut(fileName) {
        PreferenceDataStoreFactory.createWithPath(produceFile = { "${appFilesDirPath()}/$fileName".toPath() })
    }
