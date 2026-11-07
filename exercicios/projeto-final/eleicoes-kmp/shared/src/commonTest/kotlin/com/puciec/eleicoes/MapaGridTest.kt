package com.puciec.eleicoes

import com.puciec.eleicoes.domain.mapa.Ufs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Grade de UFs do Mapa (#6): cada UF num quadrado, na posição aproximada do mapa do Brasil. */
class MapaGridTest {
    private val expectedCells = mapOf(
        "RR" to (0 to 1), "AP" to (0 to 2),
        "AM" to (1 to 1), "PA" to (1 to 2), "MA" to (1 to 3), "CE" to (1 to 4), "RN" to (1 to 5),
        "AC" to (2 to 0), "RO" to (2 to 1), "TO" to (2 to 2), "PI" to (2 to 3), "PE" to (2 to 4), "PB" to (2 to 5),
        "MT" to (3 to 1), "GO" to (3 to 2), "BA" to (3 to 3), "SE" to (3 to 4), "AL" to (3 to 5),
        "MS" to (4 to 1), "DF" to (4 to 2), "MG" to (4 to 3), "ES" to (4 to 4),
        "PR" to (5 to 1), "SP" to (5 to 2), "RJ" to (5 to 3),
        "SC" to (6 to 1),
        "RS" to (7 to 1),
    )

    @Test
    fun `as 27 UFs tem celulas unicas dentro da grade`() {
        assertEquals(27, Ufs.all.size)
        assertEquals(expectedCells.keys, Ufs.all.map { it.code }.toSet())
        val cells = Ufs.all.map { it.row to it.column }
        assertEquals(27, cells.toSet().size, "duas UFs na mesma célula")
        assertTrue(Ufs.all.all { it.row in 0 until Ufs.ROWS && it.column in 0 until Ufs.COLUMNS })
        assertEquals(6, Ufs.COLUMNS)
        assertEquals(8, Ufs.ROWS)
    }

    @Test
    fun `posicoes seguem o desenho do design`() {
        expectedCells.forEach { (code, cell) ->
            val uf = Ufs.byCode(code)!!
            assertEquals(cell, uf.row to uf.column, "posição de $code")
        }
        assertEquals(0, Ufs.byCode("RR")!!.row)
        assertEquals(Ufs.ROWS - 1, Ufs.byCode("RS")!!.row)
        assertEquals(0, Ufs.byCode("AC")!!.column)
        assertTrue(Ufs.byCode("SP")!!.column < Ufs.byCode("RJ")!!.column)
    }

    @Test
    fun `exterior nao entra na grade`() {
        assertNull(Ufs.byCode("ZZ"))
        assertNull(Ufs.byCode("BR"))
        assertEquals("São Paulo", Ufs.byCode("sp")!!.name)
        assertEquals("Na", Ufs.byCode("BA")!!.preposition)
        assertEquals("Em", Ufs.byCode("SP")!!.preposition)
        assertEquals("No", Ufs.byCode("TO")!!.preposition)
    }
}
