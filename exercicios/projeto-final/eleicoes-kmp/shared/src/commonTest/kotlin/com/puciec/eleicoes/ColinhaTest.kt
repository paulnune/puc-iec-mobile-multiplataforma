package com.puciec.eleicoes

import com.puciec.eleicoes.domain.colinha.AddDecision
import com.puciec.eleicoes.domain.colinha.Colinha
import com.puciec.eleicoes.domain.favorites.FavoriteCandidate
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ColinhaTest {

    private fun fav(
        id: String,
        office: Office,
        number: Int,
        state: String = "SP",
        outcome: Outcome = Outcome.UNKNOWN,
        name: String = "CANDIDATO $id",
        party: String? = "PX",
    ) = FavoriteCandidate(
        id = id,
        ballotName = name,
        number = number,
        office = office,
        officeLabel = office.label.uppercase(),
        state = state,
        partyAcronym = party,
        photoUrl = null,
        outcome = outcome,
    )

    private val presidente = fav("p", Office.PRESIDENT, 13, state = "BR")
    private val governador = fav("g", Office.GOVERNOR, 45)
    private val senador1 = fav("s1", Office.SENATOR, 222)
    private val senador2 = fav("s2", Office.SENATOR, 151)
    private val estadual = fav("e", Office.STATE_DEPUTY, 12345)
    private val federal = fav("f", Office.FEDERAL_DEPUTY, 1234)

    // --- ordem da urna ---

    @Test
    fun `ordem da urna vai de deputado federal a presidente`() {
        val shuffled = listOf(presidente, senador1, federal, governador, estadual, senador2)
        val ordered = Colinha.urnaOrder(shuffled).map { it.id }
        assertEquals(listOf("f", "e", "s1", "s2", "g", "p"), ordered)
    }

    @Test
    fun `senadores ficam na ordem em que foram favoritados`() {
        val ordered = Colinha.urnaOrder(listOf(senador2, governador, senador1))
        assertEquals(listOf("s2", "s1", "g"), ordered.map { it.id })
        assertEquals("Senador · 1ª vaga", Colinha.seatLabel(senador2, ordered))
        assertEquals("Senador · 2ª vaga", Colinha.seatLabel(senador1, ordered))
        assertEquals("Governador", Colinha.seatLabel(governador, ordered))
    }

    @Test
    fun `distrital ocupa o lugar do estadual na ordem`() {
        val distrital = fav("d", Office.DISTRICT_DEPUTY, 54321, state = "DF")
        val ordered = Colinha.urnaOrder(listOf(fav("gdf", Office.GOVERNOR, 10, state = "DF"), distrital))
        assertEquals(listOf("d", "gdf"), ordered.map { it.id })
    }

    @Test
    fun `cargos fora da urna nao aparecem na cola`() {
        val vice = fav("v", Office.VICE_PRESIDENT, 13, state = "BR")
        assertEquals(listOf("p"), Colinha.urnaOrder(listOf(vice, presidente)).map { it.id })
    }

    @Test
    fun `numero sai com os digitos do cargo`() {
        assertEquals("0045", Colinha.urnaDigits(fav("x", Office.FEDERAL_DEPUTY, 45)))
        assertEquals("12345", Colinha.urnaDigits(estadual))
        assertEquals("222", Colinha.urnaDigits(senador1))
        assertEquals("13", Colinha.urnaDigits(presidente))
    }

    // --- limite por cargo ---

    @Test
    fun `cargo vazio favorita direto`() {
        assertEquals(AddDecision.Add, Colinha.evaluate(emptyList(), governador))
        assertEquals(AddDecision.Add, Colinha.evaluate(listOf(presidente, federal), governador))
    }

    @Test
    fun `um por cargo - segundo governador pergunta se substitui`() {
        val outro = fav("g2", Office.GOVERNOR, 13)
        val decision = Colinha.evaluate(listOf(governador, presidente), outro)
        assertIs<AddDecision.SeatFull>(decision)
        assertEquals(listOf(governador), decision.occupants)
    }

    @Test
    fun `senador aceita dois e o terceiro pergunta qual sai`() {
        assertEquals(AddDecision.Add, Colinha.evaluate(listOf(senador1), senador2))
        val terceiro = fav("s3", Office.SENATOR, 555)
        val decision = Colinha.evaluate(listOf(senador1, senador2), terceiro)
        assertIs<AddDecision.SeatFull>(decision)
        assertEquals(listOf(senador1, senador2), decision.occupants)
    }

    @Test
    fun `substituir troca o ocupante e preserva os demais`() {
        val outro = fav("g2", Office.GOVERNOR, 13)
        val result = Colinha.replace(listOf(presidente, governador, federal), listOf(governador), outro)
        assertEquals(listOf("p", "f", "g2"), result.map { it.id })
    }

    @Test
    fun `ja favoritado e cargos fora da urna`() {
        assertEquals(AddDecision.AlreadyIn, Colinha.evaluate(listOf(governador), governador))
        val suplente = fav("sup", Office.SENATOR_FIRST_ALTERNATE, 222)
        assertEquals(AddDecision.NotOnBallot, Colinha.evaluate(emptyList(), suplente))
    }

    // --- mesma UF ---

    @Test
    fun `cargo estadual de outra UF pede para trocar a UF`() {
        val governadorMg = fav("gmg", Office.GOVERNOR, 30, state = "MG")
        val decision = Colinha.evaluate(listOf(presidente, governador, senador1), governadorMg)
        assertIs<AddDecision.OtherState>(decision)
        assertEquals("SP", decision.currentState)
        assertEquals("MG", decision.newState)
        // Presidente é nacional: fica.
        assertEquals(listOf(governador, senador1), decision.toRemove)
    }

    @Test
    fun `cargos diferentes da outra UF tambem conflitam`() {
        val federalRj = fav("frj", Office.FEDERAL_DEPUTY, 2222, state = "RJ")
        assertIs<AddDecision.OtherState>(Colinha.evaluate(listOf(senador1), federalRj))
    }

    @Test
    fun `presidente nao depende da UF`() {
        assertEquals(AddDecision.Add, Colinha.evaluate(listOf(governador, senador1), presidente))
        assertEquals(null, Colinha.stateOf(listOf(presidente)))
        assertEquals("SP", Colinha.stateOf(listOf(presidente, governador)))
    }

    // --- placar e compartilhar ---

    @Test
    fun `placar conta eleitos e segundo turno`() {
        val list = listOf(
            governador.copy(outcome = Outcome.RUNOFF),
            senador1.copy(outcome = Outcome.ELECTED),
            federal.copy(outcome = Outcome.NOT_ELECTED),
            presidente.copy(outcome = Outcome.ELECTED),
        )
        val score = Colinha.scoreboard(list)
        assertEquals(4, score.total)
        assertEquals(2, score.elected)
        assertEquals(1, score.runoff)
    }

    @Test
    fun `texto para compartilhar segue a ordem da urna`() {
        val list = listOf(
            presidente.copy(ballotName = "LULA", partyAcronym = "PT", outcome = Outcome.ELECTED),
            federal.copy(ballotName = "FULANO DE TAL", partyAcronym = "PL"),
        )
        val text = Colinha.shareText(list)
        val lines = text.lines()
        assertEquals("Minha colinha · Eleições 2026 (SP)", lines.first())
        val federalLine = lines.indexOf("Deputado Federal: 1234 · Fulano de Tal (PL)")
        val presidentLine = lines.indexOf("Presidente: 13 · Lula (PT) · Eleito")
        assertTrue(federalLine in 0 until presidentLine, text)
        assertEquals("Placar: 1 de 2 eleitos", lines.last())
    }
}
