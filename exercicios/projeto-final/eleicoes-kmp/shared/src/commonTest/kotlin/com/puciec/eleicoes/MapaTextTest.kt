package com.puciec.eleicoes

import com.puciec.eleicoes.domain.mapa.formatShare
import com.puciec.eleicoes.domain.mapa.formatVotes
import com.puciec.eleicoes.domain.mapa.placeSentence
import com.puciec.eleicoes.domain.mapa.toNameProse
import com.puciec.eleicoes.domain.model.Office
import kotlin.test.Test
import kotlin.test.assertEquals

/** Textos do Mapa (#6), no formato do eleicoes.dev/f/mapa. Números reais do 1º turno de 2026. */
class MapaTextTest {
    @Test
    fun `nomes seguem a regra do site`() {
        assertEquals("Flavio Bolsonaro", "FLAVIO BOLSONARO".toNameProse())
        assertEquals("Acm Neto", "ACM NETO".toNameProse())
        assertEquals("PCB", "PCB".toNameProse())
        assertEquals("JHC", "JHC".toNameProse())
        assertEquals("Dr. Furlan", "DR. FURLAN".toNameProse())
        assertEquals("Vicentinho Júnior", "VICENTINHO JÚNIOR".toNameProse())
        assertEquals("Lula", "LULA".toNameProse())
    }

    @Test
    fun `votos usam ponto de milhar`() {
        assertEquals("56.104.503", formatVotes(56_104_503))
        assertEquals("157.387", formatVotes(157_387))
        assertEquals("999", formatVotes(999))
        assertEquals("0", formatVotes(0))
    }

    @Test
    fun `percentual usa uma casa decimal com virgula`() {
        assertEquals("62,7%", formatShare(62.652626857))
        assertEquals("0,3%", formatShare(0.347938673))
        assertEquals("100,0%", formatShare(100.0))
    }

    @Test
    fun `frase do Brasil para presidente igual ao site`() {
        val brasil = placeOf(
            "BR",
            leader = vote("FLAVIO BOLSONARO", "PL", 56_104_503, 47.03),
            runnerUp = vote("LULA", "PT", 53_879_538, 45.16),
        )
        assertEquals(
            "No Brasil, Flavio Bolsonaro teve 56.104.503 votos e Lula 53.879.538. " +
                "Diferença de 2,2 milhões de votos. 100% das seções apuradas.",
            placeSentence(brasil),
        )
    }

    @Test
    fun `diferenca abaixo de um milhao sai inteira`() {
        val tocantins = placeOf(
            "TO",
            office = Office.GOVERNOR,
            leader = vote("PROFESSORA DORINHA", "UNIÃO", 386_260, 45.52),
            runnerUp = vote("VICENTINHO JÚNIOR", "PSDB", 372_848, 43.94),
        )
        assertEquals(
            "No Tocantins, Professora Dorinha teve 386.260 votos e Vicentinho Júnior 372.848. " +
                "Diferença de 13.412 votos. 100% das seções apuradas.",
            placeSentence(tocantins),
        )
    }

    @Test
    fun `diferenca de um milhao sai no singular`() {
        fun sentence(leader: Long, runner: Long) = placeSentence(
            placeOf("BR", leader = vote("A", "PL", leader), runnerUp = vote("B", "PT", runner)),
        )
        assertEquals(
            "No Brasil, A teve 2.040.000 votos e B 1.000.000. Diferença de 1 milhão de votos. 100% das seções apuradas.",
            sentence(2_040_000, 1_000_000),
        )
        assertEquals(
            "No Brasil, A teve 2.000.000 votos e B 1.000.000. Diferença de 1 milhão de votos. 100% das seções apuradas.",
            sentence(2_000_000, 1_000_000),
        )
        assertEquals(
            "No Brasil, A teve 1.999.999 votos e B 1.000.000. Diferença de 999.999 votos. 100% das seções apuradas.",
            sentence(1_999_999, 1_000_000),
        )
        assertEquals(
            "No Brasil, A teve 13.000.000 votos e B 1.000.000. Diferença de 12 milhões de votos. 100% das seções apuradas.",
            sentence(13_000_000, 1_000_000),
        )
    }

    @Test
    fun `governador eleito no primeiro turno ganha a frase final`() {
        val saoPaulo = placeOf(
            "SP",
            office = Office.GOVERNOR,
            leader = vote("TARCÍSIO", "REPUBLICANOS", 14_491_874, 62.652626857),
            runnerUp = vote("FERNANDO HADDAD", "PT", 8_423_656, 36.417938504),
        )
        assertEquals(
            "Em São Paulo, Tarcísio teve 14.491.874 votos e Fernando Haddad 8.423.656. " +
                "Diferença de 6,1 milhões de votos. 100% das seções apuradas. A eleição foi decidida no primeiro turno.",
            placeSentence(saoPaulo),
        )
        val tocantins = placeOf(
            "TO",
            office = Office.GOVERNOR,
            leader = vote("PROFESSORA DORINHA", "UNIÃO", 386_260, 45.515767196),
            runnerUp = vote("VICENTINHO JÚNIOR", "PSDB", 372_848, 43.9353357),
        )
        assertEquals(false, placeSentence(tocantins).contains("decidida"))
        // Presidente numa UF com mais de 50% não ganha a frase: a regra é só do governador.
        val bahiaPresidente = placeOf(
            "BA",
            leader = vote("LULA", "PT", 5_664_771, 70.0),
            runnerUp = vote("FLAVIO BOLSONARO", "PL", 2_000_000, 25.0),
        )
        assertEquals(false, placeSentence(bahiaPresidente).contains("decidida"))
    }

    @Test
    fun `frase usa o artigo do estado e o exterior`() {
        val bahia = placeOf(
            "BA",
            leader = vote("LULA", "PT", 5_664_771, 70.0),
            runnerUp = vote("FLAVIO BOLSONARO", "PL", 2_000_000, 25.0),
        )
        assertEquals(
            "Na Bahia, Lula teve 5.664.771 votos e Flavio Bolsonaro 2.000.000. " +
                "Diferença de 3,7 milhões de votos. 100% das seções apuradas.",
            placeSentence(bahia),
        )
        val exterior = placeOf(
            "ZZ",
            leader = vote("LULA", "PT", 157_387, 60.0),
            runnerUp = vote("FLAVIO BOLSONARO", "PL", 100_000, 38.0),
        )
        assertEquals(
            "No exterior, Lula teve 157.387 votos e Flavio Bolsonaro 100.000. " +
                "Diferença de 57.387 votos. 100% das seções apuradas.",
            placeSentence(exterior),
        )
    }

    @Test
    fun `sem segundo colocado conta zero votos`() {
        val roraima = placeOf(
            "RR",
            office = Office.GOVERNOR,
            leader = vote("ARTHUR HENRIQUE", "PL", 220_809, 40.0),
            runnerUp = null,
        )
        assertEquals(
            "Em Roraima, Arthur Henrique teve 220.809 votos. Diferença de 220.809 votos. 100% das seções apuradas.",
            placeSentence(roraima),
        )
    }

    @Test
    fun `sem percentual apurado omite o trecho`() {
        val tocantins = placeOf(
            "TO",
            office = Office.GOVERNOR,
            leader = vote("PROFESSORA DORINHA", "UNIÃO", 386_260, 45.52),
            runnerUp = vote("VICENTINHO JÚNIOR", "PSDB", 372_848, 43.94),
            countedShare = null,
        )
        assertEquals(
            "No Tocantins, Professora Dorinha teve 386.260 votos e Vicentinho Júnior 372.848. Diferença de 13.412 votos.",
            placeSentence(tocantins),
        )
    }
}
