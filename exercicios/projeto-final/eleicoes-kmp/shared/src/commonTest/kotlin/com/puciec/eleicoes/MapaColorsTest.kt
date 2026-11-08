package com.puciec.eleicoes

import com.puciec.eleicoes.domain.mapa.MapaColors
import kotlin.test.Test
import kotlin.test.assertEquals

/** Cores do Mapa (#6): os valores esperados saem das regras do eleicoes.dev/f/mapa. */
class MapaColorsTest {
    private fun hex(rgb: Int): String = "#" + rgb.toString(16).padStart(6, '0')

    @Test
    fun `siglas da tabela usam as cores do site`() {
        assertEquals("#1f3f8b", hex(MapaColors.partyColor("PL")))
        assertEquals("#d71920", hex(MapaColors.partyColor("PT")))
        assertEquals("#e0a100", hex(MapaColors.partyColor("PSD")))
        assertEquals("#0e8a4b", hex(MapaColors.partyColor("MDB")))
        assertEquals("#0b74b8", hex(MapaColors.partyColor("PP")))
        assertEquals("#163e78", hex(MapaColors.partyColor("REPUBLICANOS")))
        assertEquals("#e36a12", hex(MapaColors.partyColor("PSB")))
        assertEquals("#c62828", hex(MapaColors.partyColor("PDT")))
        assertEquals("#1f6b45", hex(MapaColors.partyColor("PODE")))
        assertEquals("#0077c8", hex(MapaColors.partyColor("PSDB")))
        assertEquals("#f15a22", hex(MapaColors.partyColor("NOVO")))
        assertEquals("#5c068c", hex(MapaColors.partyColor("PSOL")))
        assertEquals("#9b0030", hex(MapaColors.partyColor("PCDOB")))
        assertEquals("#2e7d32", hex(MapaColors.partyColor("PV")))
        assertEquals("#00897b", hex(MapaColors.partyColor("REDE")))
        assertEquals("#ef6c00", hex(MapaColors.partyColor("AVANTE")))
        assertEquals("#f9a825", hex(MapaColors.partyColor("SOLIDARIEDADE")))
        assertEquals("#01579b", hex(MapaColors.partyColor("PRD")))
        assertEquals("#ef6c00", hex(MapaColors.partyColor("CIDADANIA")))
    }

    @Test
    fun `sigla com acento ou espacos usa a mesma cor`() {
        assertEquals("#123a7a", hex(MapaColors.partyColor(" união ")))
        assertEquals("#123a7a", hex(MapaColors.partyColor("UNIAO")))
        assertEquals("#6a1b9a", hex(MapaColors.partyColor("Missão")))
        assertEquals("#6a1b9a", hex(MapaColors.partyColor("MISSAO")))
        assertEquals("#1f3f8b", hex(MapaColors.partyColor("pl")))
    }

    @Test
    fun `sigla fora da tabela usa a paleta de reserva`() {
        assertEquals("#827717", hex(MapaColors.partyColor("UP")))
        assertEquals("#ad1457", hex(MapaColors.partyColor("PCB")))
        assertEquals("#37474f", hex(MapaColors.partyColor("PSTU")))
        assertEquals("#6d4c41", hex(MapaColors.partyColor("DC")))
    }

    @Test
    fun `margem zero clareia a cor no minimo`() {
        assertEquals("#d0d7e3", hex(MapaColors.leadFill("PL", leaderVotes = 100, runnerVotes = 100)))
    }

    @Test
    fun `margem de 28 por cento ou mais usa a cor pura`() {
        assertEquals("#d71920", hex(MapaColors.leadFill("PT", leaderVotes = 64, runnerVotes = 36)))
        assertEquals("#d71920", hex(MapaColors.leadFill("PT", leaderVotes = 90, runnerVotes = 10)))
    }

    @Test
    fun `margem do Brasil para presidente mistura como o site`() {
        assertEquals("#a1aecb", hex(MapaColors.leadFill("PL", leaderVotes = 56_104_503, runnerVotes = 53_879_538)))
        assertEquals("#9fafc6", hex(MapaColors.leadFill("UNIÃO", leaderVotes = 386_260, runnerVotes = 372_848)))
    }

    @Test
    fun `sem votos usa a cor pura do partido`() {
        assertEquals("#1f3f8b", hex(MapaColors.leadFill("PL", leaderVotes = 0, runnerVotes = 0)))
    }
}
