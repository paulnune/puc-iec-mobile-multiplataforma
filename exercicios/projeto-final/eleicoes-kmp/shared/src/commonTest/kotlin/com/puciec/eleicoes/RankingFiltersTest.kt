package com.puciec.eleicoes

import com.puciec.eleicoes.data.source.mcp.McpArguments
import com.puciec.eleicoes.domain.filter.CandidateFilter
import com.puciec.eleicoes.domain.filter.Gender
import com.puciec.eleicoes.domain.filter.RankingCategory
import com.puciec.eleicoes.domain.filter.rankingFilter
import com.puciec.eleicoes.domain.model.Office
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RankingFiltersTest {
    @Test
    fun `chips mantem os rotulos da referencia`() {
        assertEquals(
            listOf(
                "Todos",
                "Executivo",
                "Legislativo",
                "Presidência",
                "Governadores",
                "Senado",
                "Câmara",
                "Assembleias",
                "Mulheres",
            ),
            RankingCategory.entries.map { it.label },
        )
    }

    @Test
    fun `cada chip vira os parametros office e gender da api`() {
        val expectedOffices = mapOf(
            RankingCategory.ALL to emptySet(),
            RankingCategory.EXECUTIVE to
                setOf("president", "governor", "vicePresident", "viceGovernor"),
            RankingCategory.LEGISLATIVE to setOf(
                "senator",
                "federalDeputy",
                "stateDeputy",
                "districtDeputy",
                "senatorFirstAlternate",
                "senatorSecondAlternate",
            ),
            RankingCategory.PRESIDENCY to setOf("president"),
            RankingCategory.GOVERNORS to setOf("governor"),
            RankingCategory.SENATE to setOf("senator", "senatorFirstAlternate", "senatorSecondAlternate"),
            RankingCategory.CHAMBER to setOf("federalDeputy"),
            RankingCategory.ASSEMBLIES to setOf("stateDeputy", "districtDeputy"),
            RankingCategory.WOMEN to emptySet(),
        )
        val expectedGender = mapOf(
            RankingCategory.WOMEN to Gender.FEMININO.apiValue,
        )

        RankingCategory.entries.forEach { category ->
            val args = McpArguments.candidates(rankingFilter(category, "sp"), page = 1, limit = 20)

            assertEquals(expectedOffices.getValue(category), args.stringSet("office"), "office do chip ${category.label}")
            val gender = expectedGender[category]
            if (gender != null) {
                assertEquals(setOf(gender), args.stringSet("gender"), "gender do chip ${category.label}")
            } else {
                assertNull(args["gender"], "gender do chip ${category.label}")
            }
            assertEquals(setOf("SP"), args.stringSet("state"), "state do chip ${category.label}")
            assertNull(args["q"], "busca local não deve mandar q para a API")
        }
    }

    @Test
    fun `chip preserva a uf e o partido ja escolhidos`() {
        val base = CandidateFilter(states = setOf("RJ"), partyIds = setOf("13"))

        val filter = RankingCategory.SENATE.toFilter(base)

        assertEquals(RankingCategory.SENATE.offices, filter.offices)
        assertEquals(setOf("RJ"), filter.states)
        assertEquals(setOf("13"), filter.partyIds)
    }

    @Test
    fun `mulheres limpa os cargos e liga o genero`() {
        val filter = RankingCategory.WOMEN.toFilter(CandidateFilter(offices = setOf(Office.SENATOR)))

        assertTrue(filter.offices.isEmpty())
        assertEquals(Gender.FEMININO, filter.gender)
    }

    @Test
    fun `todos limpa office e gender`() {
        val filter = RankingCategory.ALL.toFilter(
            CandidateFilter(offices = setOf(Office.SENATOR), gender = Gender.FEMININO),
        )

        assertTrue(filter.offices.isEmpty())
        assertNull(filter.gender)
    }

    @Test
    fun `uf vazia vira todas e uf minuscula vira maiuscula`() {
        assertEquals(emptySet<String>(), CandidateFilter().withState(null).states)
        assertEquals(emptySet<String>(), CandidateFilter().withState("   ").states)
        assertEquals(setOf("SP"), CandidateFilter().withState("sp").states)
    }

    @Test
    fun `of reconhece o chip selecionado`() {
        RankingCategory.entries.forEach { category ->
            assertEquals(category, RankingCategory.of(rankingFilter(category, "MG")), "chip ${category.label}")
        }
    }
}

private fun JsonObject.stringSet(key: String): Set<String> =
    (this[key] as? JsonArray)?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet()
