package com.puciec.eleicoes.ui.theme

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.Test
import com.puciec.eleicoes.platform.sha256Hex
import com.puciec.eleicoes.testTempDirPath
import kotlin.random.Random
import okio.Path.Companion.toPath

class ThemeStoreTest {

    private fun newFile(name: String) = (testTempDirPath() + "/" + sha256Hex(Random.nextLong().toString()).take(12) + "-" + name).toPath()

    @Test
    fun `valor ausente ou desconhecido cai em sistema`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStorage(null))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStorage("ROXO"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStorage(""))
    }

    @Test
    fun `valor gravado volta igual e sobrevive a reabrir o app`() = runTest {
        val file = newFile("tema.preferences_pb")
        // Sessão 1 com escopo próprio, para poder encerrá-la antes de reabrir o arquivo.
        val session1 = CoroutineScope(Job() + StandardTestDispatcher(testScheduler))
        val store = ThemeStore(
            PreferenceDataStoreFactory.createWithPath(scope = session1, produceFile = { file }),
        )
        assertEquals(ThemePreference.SYSTEM, store.preference.first())

        store.save(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, store.preference.first())

        // Fecha a sessão 1 (como o processo morrendo) e reabre sobre o mesmo arquivo.
        session1.cancel()
        val reopened = ThemeStore(
            PreferenceDataStoreFactory.createWithPath(scope = backgroundScope, produceFile = { file }),
        )
        assertEquals(ThemePreference.DARK, reopened.preference.first())

        reopened.save(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, reopened.preference.first())
    }
}
