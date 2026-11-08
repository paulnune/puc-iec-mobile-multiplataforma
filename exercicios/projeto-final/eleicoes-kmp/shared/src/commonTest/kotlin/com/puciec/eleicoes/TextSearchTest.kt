package com.puciec.eleicoes

import com.puciec.eleicoes.domain.filter.matchingText
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Office
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextSearchTest {
    private val candidates = listOf(
        candidate(id = "1", ballotName = "JOÃO DA SILVA", fullName = "João da Silva"),
        candidate(id = "2", ballotName = "MARIA SOUZA", fullName = "Maria Conceição Souza"),
        candidate(id = "3", ballotName = "ANA LÚCIA", fullName = "Ana Lúcia Ferreira"),
    )

    @Test
    fun `procura sem diferenciar maiuscula nem acento`() {
        assertEquals(listOf("1"), candidates.matchingText("joao").map { it.id })
        assertEquals(listOf("1"), candidates.matchingText("JOÃO").map { it.id })
        assertEquals(listOf("3"), candidates.matchingText("lucia").map { it.id })
    }

    @Test
    fun `encontra pelo nome completo quando o nome de urna nao tem o termo`() {
        assertEquals(listOf("2"), candidates.matchingText("conceicao").map { it.id })
    }

    @Test
    fun `texto vazio devolve a lista inteira`() {
        assertEquals(candidates, candidates.matchingText("   "))
    }

    @Test
    fun `termo sem correspondencia devolve lista vazia`() {
        assertTrue(candidates.matchingText("xyz").isEmpty())
    }

    @Test
    fun `ignora espacos nas pontas`() {
        assertEquals(listOf("2"), candidates.matchingText("  maria  ").map { it.id })
    }
}

private fun candidate(id: String, ballotName: String, fullName: String): Candidate = Candidate(
    id = id,
    ballotName = ballotName,
    fullName = fullName,
    number = 1,
    office = Office.PRESIDENT,
    officeLabel = "Presidente",
    state = "SP",
    partyId = "1",
    partyAcronym = "PL",
    photoUrl = null,
    totalAssets = 0.0,
    assetsDisclosed = true,
    gender = null,
    birthDate = null,
    birthCity = null,
    birthState = null,
    nationality = null,
    education = null,
    occupation = null,
    result = null,
)
