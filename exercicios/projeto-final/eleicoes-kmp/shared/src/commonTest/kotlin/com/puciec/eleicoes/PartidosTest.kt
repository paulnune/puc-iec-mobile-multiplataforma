package com.puciec.eleicoes

import com.puciec.eleicoes.domain.model.Party
import com.puciec.eleicoes.domain.partidos.PartySort
import com.puciec.eleicoes.domain.partidos.displayName
import com.puciec.eleicoes.domain.partidos.filterAndSort
import com.puciec.eleicoes.domain.partidos.officialParties
import com.puciec.eleicoes.domain.partidos.updatedAgo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Busca e ordenação dos partidos (#8). Partidos reais do eleicoes.dev (list_parties, 07/11). */
class PartidosTest {

    private val republicanos = Party("rep", 10, "REPUBLICANOS", "REPUBLICANOS")
    private val pp = Party("pp", 11, "PP", "PROGRESSISTAS")
    private val pdt = Party("pdt", 12, "PDT", "PARTIDO DEMOCRÁTICO TRABALHISTA")
    private val pstu = Party("pstu", 16, "PSTU", "PARTIDO SOCIALISTA  DOS TRABALHADORES UNIFICADO")
    private val psl = Party("psl", 17, "PSL", "PARTIDO SOCIAL LIBERAL")
    private val psb = Party("psb", 40, "PSB", "PARTIDO SOCIALISTA BRASILEIRO")
    private val psdb = Party("psdb", 45, "PSDB", "PARTIDO DA SOCIAL DEMOCRACIA BRASILEIRA")
    private val psol = Party("psol", 50, "PSOL", "PARTIDO SOCIALISMO E LIBERDADE")
    private val psd = Party("psd", 55, "PSD", "PARTIDO SOCIAL DEMOCRÁTICO")
    private val pros = Party("pros", 90, "PROS", "PARTIDO REPUBLICANO DA ORDEM SOCIAL")

    /** Fora de ordem de propósito: a ordenação é responsabilidade de filterAndSort. */
    private val all = listOf(psd, pp, psdb, republicanos, pstu, pros, pdt, psol, psb, psl)

    // --- Busca ---

    @Test
    fun `busca pela sigla sem diferenciar maiusculas`() {
        assertEquals(listOf(psdb), all.filterAndSort("psdb", PartySort.NUMBER))
    }

    @Test
    fun `busca pelo nome sem acento`() {
        assertEquals(listOf(pdt, psd), all.filterAndSort("democratico", PartySort.NUMBER))
    }

    @Test
    fun `busca ignora espacos extras e acha os 7 sociais`() {
        assertEquals(
            listOf(pstu, psl, psb, psdb, psol, psd, pros),
            all.filterAndSort("  SOCIAL ", PartySort.NUMBER),
        )
    }

    @Test
    fun `busca pelo numero do partido`() {
        assertEquals(listOf(psdb), all.filterAndSort("45", PartySort.NUMBER))
    }

    @Test
    fun `busca sem resultado e busca vazia`() {
        assertTrue(all.filterAndSort("xyz", PartySort.NUMBER).isEmpty())
        assertEquals(all.size, all.filterAndSort("   ", PartySort.NUMBER).size)
    }

    // --- Ordem ---

    @Test
    fun `ordem pelo numero da urna`() {
        assertEquals(listOf(10, 11, 12, 16, 17, 40, 45, 50, 55, 90), all.filterAndSort("", PartySort.NUMBER).map { it.number })
    }

    @Test
    fun `ordem pela sigla de A a Z`() {
        assertEquals(
            listOf("PDT", "PP", "PROS", "PSB", "PSD", "PSDB", "PSL", "PSOL", "PSTU", "REPUBLICANOS"),
            all.filterAndSort("", PartySort.ACRONYM).map { it.acronym },
        )
    }

    @Test
    fun `empate na sigla desempata pelo numero`() {
        val a = Party("a", 70, "AVANTE", "AVANTE")
        val b = Party("b", 7, "AVANTE", "AVANTE")
        assertEquals(listOf(7, 70), listOf(a, b).filterAndSort("", PartySort.ACRONYM).map { it.number })
    }

    @Test
    fun `filtro mantem a ordem escolhida`() {
        assertEquals(
            listOf("PDT", "PSD"),
            all.filterAndSort("democratico", PartySort.ACRONYM).map { it.acronym },
        )
    }

    // --- Nome de exibição ---

    @Test
    fun `nome igual a sigla aparece em title case embaixo`() {
        // A API manda REPUBLICANOS / REPUBLICANOS (idem CIDADANIA, DEMOCRATA, AGIR, AVANTE): a sigla oficial é essa.
        assertEquals("Republicanos", republicanos.displayName())
        assertEquals("Avante", Party("av", 70, "AVANTE", "AVANTE").displayName())
    }

    @Test
    fun `nome em title case com espaco simples`() {
        assertEquals("Partido Socialista dos Trabalhadores Unificado", pstu.displayName())
        assertEquals("Progressistas", pp.displayName())
    }

    @Test
    fun `nome em branco nao aparece`() {
        assertNull(Party("x", 1, "X", "  ").displayName())
    }

    // --- Partidos oficiais de 2026 (revisão 4) ---

    /** Os 38 partidos exatamente como a tool list_parties devolveu em 07/11. */
    private val api38 = listOf(
        Party("p10", 10, "REPUBLICANOS", "REPUBLICANOS"),
        Party("p11", 11, "PP", "PROGRESSISTAS"),
        Party("p12", 12, "PDT", "PARTIDO DEMOCRÁTICO TRABALHISTA"),
        Party("p13", 13, "PT", "PARTIDO DOS TRABALHADORES"),
        Party("p14", 14, "MISSÃO", "PARTIDO MISSÃO"),
        Party("p15", 15, "MDB", "MOVIMENTO DEMOCRÁTICO BRASILEIRO"),
        Party("p16", 16, "PSTU", "PARTIDO SOCIALISTA  DOS TRABALHADORES UNIFICADO"),
        Party("p17", 17, "PSL", "PARTIDO SOCIAL LIBERAL"),
        Party("p18", 18, "REDE", "REDE SUSTENTABILIDADE"),
        Party("p19", 19, "PTN", "PARTIDO TRABALHISTA NACIONAL"),
        Party("p20", 20, "PODE", "PODEMOS"),
        Party("p21", 21, "PCB", "PARTIDO COMUNISTA BRASILEIRO"),
        Party("p22", 22, "PL", "PARTIDO LIBERAL"),
        Party("p23", 23, "CIDADANIA", "CIDADANIA"),
        Party("p25", 25, "PRD", "PARTIDO RENOVACAO DEMOCRATICA"),
        Party("p26", 26, "PAN", "PARTIDO DOS APOSENTADOS DA NAÇÃO"),
        Party("p27", 27, "DC", "DEMOCRACIA CRISTÃ"),
        Party("p28", 28, "PRTB", "PARTIDO RENOVADOR TRABALHISTA BRASILEIRO"),
        Party("p29", 29, "PCO", "PARTIDO DA CAUSA OPERÁRIA"),
        Party("p30", 30, "NOVO", "PARTIDO NOVO"),
        Party("p31", 31, "PHS", "PARTIDO HUMANISTA DA SOLIDARIEDADE"),
        Party("p33", 33, "MOBILIZA", "MOBILIZAÇÃO NACIONAL"),
        Party("p35", 35, "DEMOCRATA", "DEMOCRATA"),
        Party("p36", 36, "AGIR", "AGIR"),
        Party("p40", 40, "PSB", "PARTIDO SOCIALISTA BRASILEIRO"),
        Party("p43", 43, "PV", "PARTIDO VERDE"),
        Party("p44", 44, "UNIÃO", "UNIÃO BRASIL"),
        Party("p45", 45, "PSDB", "PARTIDO DA SOCIAL DEMOCRACIA BRASILEIRA"),
        Party("p50", 50, "PSOL", "PARTIDO SOCIALISMO E LIBERDADE"),
        Party("p51", 51, "PATRIOTA", "PARTIDO ECOLÓGICO NACIONAL"),
        Party("p54", 54, "PPL", "PARTIDO PÁTRIA LIVRE"),
        Party("p55", 55, "PSD", "PARTIDO SOCIAL DEMOCRÁTICO"),
        Party("p56", 56, "PRONA", "PARTIDO DE REEDIFICAÇÃO DA ORDEM NACIONAL"),
        Party("p65", 65, "PC do B", "PARTIDO COMUNISTA DO BRASIL"),
        Party("p70", 70, "AVANTE", "AVANTE"),
        Party("p77", 77, "SD", "SOLIDARIEDADE"),
        Party("p80", 80, "UP", "UNIDADE POPULAR"),
        Party("p90", 90, "PROS", "PARTIDO REPUBLICANO DA ORDEM SOCIAL"),
    )

    @Test
    fun `lista oficial tem os 30 partidos de 2026 na ordem por sigla`() {
        val rows = api38.officialParties().filterAndSort("", PartySort.ACRONYM).map { "${it.acronym} | ${it.displayName()}" }
        assertEquals(
            TABELA_OFICIAL_2026,
            rows,
        )
    }

    @Test
    fun `partidos sem candidaturas em 2026 ficam fora`() {
        val numbers = api38.officialParties().map { it.number }.toSet()
        listOf(17, 19, 26, 31, 51, 54, 56, 90).forEach { assertTrue(it !in numbers, "partido $it deveria estar oculto") }
        assertEquals(30, numbers.size)
    }

    @Test
    fun `partido que a tabela nao conhece aparece como a API mandou`() {
        val novo = Party("p99", 99, "PNX", "PARTIDO NOVO EXEMPLO")
        val official = (api38 + novo).officialParties()

        assertEquals(novo, official.first { it.number == 99 })
        assertEquals("Partido Novo Exemplo", official.first { it.number == 99 }.displayName())
        assertEquals(31, official.size)
    }

    @Test
    fun `partido novo que reaproveita o numero de um extinto aparece`() {
        // O TSE reaproveita números: um partido futuro com o 17 não pode sumir só porque o PSL usava esse número.
        val novo17 = Party("novo17", 17, "PNF", "PARTIDO NOVO FUTURO")

        assertEquals(novo17, listOf(novo17).officialParties().single())
    }

    @Test
    fun `correcao so vale para a sigla conhecida daquele numero`() {
        // Se outro partido assumir o 77 ou o 65, ele não pode virar SOLIDARIEDADE nem PCdoB.
        val outro77 = Party("o77", 77, "XYZ", "PARTIDO XYZ")
        val outro65 = Party("o65", 65, "ABC", "PARTIDO ABC")
        val outro25 = Party("o25", 25, "QQ", "PARTIDO QQ")

        assertEquals(listOf(outro77, outro65, outro25), listOf(outro77, outro65, outro25).officialParties())
    }

    @Test
    fun `siglas e nome corrigidos continuam buscaveis`() {
        val official = api38.officialParties()
        assertEquals(listOf("PCdoB"), official.filterAndSort("pcdob", PartySort.NUMBER).map { it.acronym })
        assertEquals(listOf(77), official.filterAndSort("solidariedade", PartySort.NUMBER).map { it.number })
        assertEquals(listOf(25), official.filterAndSort("renovacao democratica", PartySort.NUMBER).map { it.number })
    }

    // --- "Atualizado há…" (revisão 8) ---

    @Test
    fun `texto de quando os dados foram buscados`() {
        val now = 10_000_000_000L
        assertEquals("Atualizado agora", updatedAgo(now - 30_000, now))
        assertEquals("Atualizado há 1 min", updatedAgo(now - 60_000, now))
        assertEquals("Atualizado há 45 min", updatedAgo(now - 45 * 60_000L, now))
        assertEquals("Atualizado há 1 h", updatedAgo(now - 60 * 60_000L, now))
        assertEquals("Atualizado há 23 h", updatedAgo(now - 23 * 3_600_000L, now))
        assertEquals("Atualizado há 1 dia", updatedAgo(now - 24 * 3_600_000L, now))
        assertEquals("Atualizado há 3 dias", updatedAgo(now - 72 * 3_600_000L, now))
        assertEquals("Atualizado agora", updatedAgo(now + 60_000, now)) // relógio voltou no tempo
    }
}
