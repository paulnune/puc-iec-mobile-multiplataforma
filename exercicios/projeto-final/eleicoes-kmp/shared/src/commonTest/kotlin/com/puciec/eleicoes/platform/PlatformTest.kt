package com.puciec.eleicoes.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun sha256HexMatchesKnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc"))
    }

    @Test
    fun sha256HexOfEmptyTextMatchesKnownVector() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256Hex(""))
    }

    @Test
    fun formatDayMonthTimeUsesPtBrLayout() {
        val text = formatDayMonthTime(1_700_000_000_000L)
        assertTrue(Regex("""\d{2}/\d{2} às \d{2}:\d{2}""").matches(text), "formato inesperado: $text")
    }
}
