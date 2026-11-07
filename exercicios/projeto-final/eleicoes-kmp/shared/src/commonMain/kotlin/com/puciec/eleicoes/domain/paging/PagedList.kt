package com.puciec.eleicoes.domain.paging

import com.puciec.eleicoes.domain.model.Page

/**
 * Lista paginada acumulada (paginação infinita, #2): junta as páginas na ordem em que chegam,
 * sem repetir item (a API pode repetir um item na virada de página) e sabe se ainda há mais.
 */
data class PagedList<T>(
    val items: List<T> = emptyList(),
    val lastPage: Int = 0,
    val endReached: Boolean = false,
) {
    val nextPage: Int get() = lastPage + 1

    /** Junta [page] ao que já foi carregado. [key] identifica o item (o id do candidato). */
    fun append(page: Page<T>, key: (T) -> Any): PagedList<T> {
        val seen = items.mapTo(HashSet()) { key(it) }
        val merged = items + page.items.filter { seen.add(key(it)) }
        return PagedList(
            items = merged,
            lastPage = page.page,
            endReached = page.items.isEmpty() || !page.hasNext,
        )
    }
}
