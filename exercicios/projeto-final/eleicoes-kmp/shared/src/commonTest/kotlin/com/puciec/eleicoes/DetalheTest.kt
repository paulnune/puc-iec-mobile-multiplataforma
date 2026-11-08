package com.puciec.eleicoes

import com.puciec.eleicoes.domain.detalhe.AssetGroup
import com.puciec.eleicoes.domain.detalhe.DetalheFormat
import com.puciec.eleicoes.domain.detalhe.OTHER_ASSET_TYPE
import com.puciec.eleicoes.domain.detalhe.ageLabel
import com.puciec.eleicoes.domain.detalhe.birthplaceLabel
import com.puciec.eleicoes.domain.detalhe.candidateAge
import com.puciec.eleicoes.domain.detalhe.shortAssetType
import com.puciec.eleicoes.domain.detalhe.summarize
import com.puciec.eleicoes.domain.model.Asset
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.Office
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Regras do Detalhe (#3). Os valores vêm de candidatos reais consultados no eleicoes.dev. */
class DetalheTest {

    // --- Idade (referência: 1º turno, 04/10/2026) ---

    @Test
    fun `idade completa na data da eleicao`() {
        assertEquals(69, candidateAge("1957-03-25"))
        assertEquals(43, candidateAge("1983-08-29"))
    }

    @Test
    fun `aniversario no dia da eleicao conta, depois dela nao`() {
        assertEquals(40, candidateAge("1986-10-04"))
        assertEquals(39, candidateAge("1986-10-05"))
        assertEquals(39, candidateAge("1986-12-31"))
    }

    @Test
    fun `aceita data com hora e o formato brasileiro`() {
        assertEquals(69, candidateAge("1957-03-25T00:00:00.000Z"))
        assertEquals(69, candidateAge("25/03/1957"))
    }

    @Test
    fun `data ausente ou invalida nao tem idade`() {
        assertNull(candidateAge(null))
        assertNull(candidateAge(""))
        assertNull(candidateAge("março de 1957"))
        assertNull(candidateAge("1957-13-01"))
        assertNull(candidateAge("2030-01-01"))
    }

    @Test
    fun `rotulo da idade`() {
        assertEquals("69 anos", candidate(birthDate = "1957-03-25").ageLabel())
        assertNull(candidate(birthDate = null).ageLabel())
    }

    // --- Naturalidade ---

    @Test
    fun `naturalizado mostra a cidade de nascimento`() {
        val c = candidate(nationality = "BRASILEIRA (NATURALIZADA)", birthCity = "ALPINÓPOLIS", birthState = "MG")
        assertEquals("Naturalizado(a) · nascido(a) em Alpinópolis", c.birthplaceLabel())
    }

    @Test
    fun `naturalizado sem cidade`() {
        val c = candidate(nationality = "BRASILEIRA (NATURALIZADA)", birthCity = null, birthState = "ZZ")
        assertEquals("Naturalizado(a)", c.birthplaceLabel())
    }

    @Test
    fun `brasileiro nato mostra cidade e UF`() {
        val c = candidate(nationality = "BRASILEIRA NATA", birthCity = "SÃO TIAGO", birthState = "mg")
        assertEquals("São Tiago (MG)", c.birthplaceLabel())
    }

    @Test
    fun `so cidade, UF ZZ ignorada e sem dados`() {
        assertEquals("Belo Horizonte", candidate(birthCity = "BELO HORIZONTE", birthState = null).birthplaceLabel())
        assertEquals("Lisboa", candidate(birthCity = "LISBOA", birthState = "ZZ").birthplaceLabel())
        assertEquals("SP", candidate(birthCity = " ", birthState = "SP").birthplaceLabel())
        assertNull(candidate(birthCity = null, birthState = null).birthplaceLabel())
    }

    // --- Bens declarados ---

    @Test
    fun `tipo curto corta no dois pontos`() {
        assertEquals(
            "Veículo automotor terrestre",
            shortAssetType("Veículo automotor terrestre: caminhão, automóvel, moto, etc."),
        )
        assertEquals("Terreno", shortAssetType("Terreno"))
        assertEquals(OTHER_ASSET_TYPE, shortAssetType("  "))
    }

    @Test
    fun `tipo todo em maiusculas vira so a primeira maiuscula`() {
        assertEquals("Outros bens e direitos", shortAssetType("OUTROS BENS E DIREITOS"))
        assertEquals("Casa", shortAssetType("Casa"))
    }

    @Test
    fun `soma, agrupa pelo tipo curto e ordena`() {
        val summary = listOf(
            asset(order = 3, type = "Terreno", value = 2_500.0),
            asset(order = 1, type = "Veículo automotor terrestre: caminhão, automóvel, moto, etc.", value = 14_695.2),
            asset(order = 2, type = "Terreno", value = 2_000.0),
            asset(order = 4, type = "", value = 300.0),
        ).summarize()

        assertEquals(listOf(1, 2, 3, 4), summary.assets.map { it.order })
        assertEquals(19_495.2, summary.total, 0.001)
        assertEquals(
            listOf(
                AssetGroup("Veículo automotor terrestre", 1, 14_695.2),
                AssetGroup("Terreno", 2, 4_500.0),
                AssetGroup(OTHER_ASSET_TYPE, 1, 300.0),
            ),
            summary.groups,
        )
    }

    @Test
    fun `empate no total ordena pelo nome do tipo`() {
        val groups = listOf(
            asset(order = 1, type = "Terreno", value = 100.0),
            asset(order = 2, type = "Apartamento", value = 100.0),
        ).summarize().groups

        assertEquals(listOf("Apartamento", "Terreno"), groups.map { it.typeLabel })
    }

    @Test
    fun `lista cortada sabe quantos bens existem`() {
        val two = listOf(asset(order = 1, type = "Terreno", value = 1.0), asset(order = 2, type = "Terreno", value = 1.0))

        assertTrue(two.summarize(declaredCount = 42).isTruncated)
        assertEquals(42, two.summarize(declaredCount = 42).declaredCount)
        assertEquals(false, two.summarize().isTruncated)
        assertEquals(2, two.summarize(declaredCount = 1).declaredCount)
    }

    @Test
    fun `sem bens, total zero e sem grupos`() {
        val summary = emptyList<Asset>().summarize()
        assertEquals(0.0, summary.total)
        assertTrue(summary.groups.isEmpty())
        assertTrue(summary.assets.isEmpty())
    }

    // --- Formatação ---

    @Test
    fun `votos com separador de milhar`() {
        assertEquals("4.968.829 votos", DetalheFormat.votes(4_968_829))
        assertEquals("607 votos", DetalheFormat.votes(607))
        assertEquals("1 voto", DetalheFormat.votes(1))
        assertEquals("0 votos", DetalheFormat.votes(0))
    }

    @Test
    fun `porcentagem ja vem de 0 a 100`() {
        assertEquals("24,2%", DetalheFormat.percent(24.208094269))
        assertEquals("< 0,1%", DetalheFormat.percent(0.00256069))
        assertEquals("0,1%", DetalheFormat.percent(0.1))
        assertEquals("0,0%", DetalheFormat.percent(0.0))
        assertEquals("100,0%", DetalheFormat.percent(100.0))
        assertEquals("50,1%", DetalheFormat.percent(50.06))
    }

    @Test
    fun `turno`() {
        assertEquals("1º turno", DetalheFormat.round(1))
        assertEquals("2º turno", DetalheFormat.round(2))
    }

    private fun asset(order: Int, type: String, value: Double) =
        Asset(id = "a$order", order = order, typeLabel = type, description = "BEM $order", value = value)

    private fun candidate(
        birthDate: String? = null,
        birthCity: String? = null,
        birthState: String? = null,
        nationality: String? = "BRASILEIRA NATA",
    ) = Candidate(
        id = "c1",
        ballotName = "DOMINGOS SÁVIO",
        fullName = "DOMINGOS SAVIO CAMPOS RESENDE",
        number = 222,
        office = Office.fromApi("senator"),
        officeLabel = "SENADOR",
        state = "MG",
        partyId = "p1",
        partyAcronym = "PSDB",
        photoUrl = null,
        totalAssets = 2_403_414.92,
        assetsDisclosed = true,
        gender = "MASCULINO",
        birthDate = birthDate,
        birthCity = birthCity,
        birthState = birthState,
        nationality = nationality,
        education = "SUPERIOR COMPLETO",
        occupation = "VETERINÁRIO",
        result = null,
    )
}
