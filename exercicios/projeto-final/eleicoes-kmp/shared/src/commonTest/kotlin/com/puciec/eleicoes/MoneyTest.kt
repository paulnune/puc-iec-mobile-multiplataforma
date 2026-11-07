package com.puciec.eleicoes

import com.puciec.eleicoes.domain.format.Money
import com.puciec.eleicoes.domain.format.toTitleCase
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {
    @Test
    fun `bilhoes saem com duas casas`() {
        assertEquals("R$ 1,23 bi", Money.compact(1_226_983_360.0))
    }

    @Test
    fun `milhoes saem com uma casa`() {
        assertEquals("R$ 242,6 mi", Money.compact(242_593_012.0))
        assertEquals("R$ 8,2 mi", Money.compact(8_200_000.0))
    }

    @Test
    fun `abaixo de um milhao mostra o valor inteiro com separador`() {
        assertEquals("R$ 795.089", Money.compact(795_089.0))
        assertEquals("R$ 0", Money.compact(0.0))
    }

    @Test
    fun `valor completo tem centavos`() {
        assertEquals("R$ 6.788.770,05", Money.full(6_788_770.05))
    }

    @Test
    fun `variacao leva sinal`() {
        assertEquals("+R$ 6,4 mi", Money.signedCompact(6_400_000.0))
        assertEquals("−R$ 2,6 mi", Money.signedCompact(-2_600_000.0))
        assertEquals("R$ 0", Money.signedCompact(0.0))
    }

    @Test
    fun `nomes em maiusculas viram titulo`() {
        assertEquals("Dra. Georgia Micheletti", "DRA. GEORGIA MICHELETTI".toTitleCase())
        assertEquals("Partido da Causa Operária", "PARTIDO DA CAUSA OPERÁRIA".toTitleCase())
    }
}
