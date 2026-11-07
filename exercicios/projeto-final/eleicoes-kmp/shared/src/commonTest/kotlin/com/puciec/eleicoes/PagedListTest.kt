package com.puciec.eleicoes

import com.puciec.eleicoes.domain.model.Page
import com.puciec.eleicoes.domain.paging.PagedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Paginação infinita do ranking (#2). */
class PagedListTest {
    private fun page(number: Int, vararg ids: String, totalPages: Int? = 3) =
        Page(items = ids.toList(), page = number, totalPages = totalPages, total = null)

    @Test
    fun `junta as paginas na ordem`() {
        val list = PagedList<String>()
            .append(page(1, "a", "b")) { it }
            .append(page(2, "c", "d")) { it }

        assertEquals(listOf("a", "b", "c", "d"), list.items)
        assertEquals(3, list.nextPage)
        assertFalse(list.endReached)
    }

    @Test
    fun `nao repete item que volta na virada de pagina`() {
        val list = PagedList<String>()
            .append(page(1, "a", "b")) { it }
            .append(page(2, "b", "c")) { it }

        assertEquals(listOf("a", "b", "c"), list.items)
    }

    @Test
    fun `chega ao fim na ultima pagina`() {
        val list = PagedList<String>()
            .append(page(1, "a")) { it }
            .append(page(2, "b")) { it }
            .append(page(3, "c")) { it }

        assertTrue(list.endReached)
    }

    @Test
    fun `pagina vazia ou sem total tambem encerram`() {
        assertTrue(PagedList<String>().append(page(1)) { it }.endReached)
        assertTrue(PagedList<String>().append(page(1, "a", totalPages = null)) { it }.endReached)
    }
}
