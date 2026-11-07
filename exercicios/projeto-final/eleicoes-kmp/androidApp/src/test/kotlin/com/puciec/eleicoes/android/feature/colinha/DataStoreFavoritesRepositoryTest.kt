package com.puciec.eleicoes.android.feature.colinha

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.puciec.eleicoes.domain.colinha.AddDecision
import com.puciec.eleicoes.domain.colinha.replace
import com.puciec.eleicoes.domain.colinha.toggleInColinha
import com.puciec.eleicoes.domain.favorites.FavoriteCandidate
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreFavoritesRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun fav(id: String, office: Office, number: Int, state: String = "SP") = FavoriteCandidate(
        id = id,
        ballotName = "CANDIDATO $id",
        number = number,
        office = office,
        officeLabel = office.label.uppercase(),
        state = state,
        partyAcronym = "PX",
        photoUrl = "https://exemplo/$id.jpg",
        outcome = Outcome.ELECTED,
    )

    @Test
    fun `colinha sobrevive a fechar e reabrir o app`() = runTest {
        val file = tmp.newFile("colinha.preferences_pb")
        val session1 = CoroutineScope(Job() + StandardTestDispatcher(testScheduler))
        val repo = DataStoreFavoritesRepository(PreferenceDataStoreFactory.create(scope = session1, produceFile = { file }))
        assertTrue(repo.favorites.first().isEmpty())

        val governador = fav("g", Office.GOVERNOR, 45)
        val senador = fav("s", Office.SENATOR, 222)
        repo.add(governador)
        repo.add(senador)
        repo.add(governador) // repetido não duplica
        assertEquals(listOf("s", "g"), repo.favorites.first().map { it.id })

        // Fecha a sessão 1 (como o processo morrendo) e reabre sobre o mesmo arquivo.
        session1.cancel()
        val reopened = DataStoreFavoritesRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }),
        )
        assertEquals(listOf(senador, governador), reopened.favorites.first())

        reopened.remove("s")
        assertEquals(listOf(governador), reopened.favorites.first())
    }

    @Test
    fun `estrela respeita o limite do cargo e a troca grava`() = runTest {
        val file = tmp.newFile("colinha.preferences_pb")
        val repo = DataStoreFavoritesRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
        val governador = fav("g", Office.GOVERNOR, 45)
        val outro = fav("g2", Office.GOVERNOR, 13)

        assertNull(repo.toggleInColinha(governador))
        val decision = repo.toggleInColinha(outro)
        assertTrue(decision is AddDecision.SeatFull)
        assertEquals(listOf(governador), repo.favorites.first()) // nada muda até o usuário confirmar

        repo.replace((decision as AddDecision.SeatFull).occupants, outro)
        assertEquals(listOf(outro), repo.favorites.first())

        assertNull(repo.toggleInColinha(outro)) // tocar de novo tira
        assertTrue(repo.favorites.first().isEmpty())
    }
}
