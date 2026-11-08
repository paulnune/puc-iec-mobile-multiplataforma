package com.puciec.eleicoes.domain.mapa

/**
 * Uma UF no Mapa (#6): nome com acento, artigo usado na frase ("Em São Paulo", "Na Bahia")
 * e a célula da grade ([row], [column]), na posição aproximada do mapa do Brasil.
 */
data class Uf(
    val code: String,
    val name: String,
    val preposition: String,
    val row: Int,
    val column: Int,
)

/** As 27 UFs da grade. O exterior ("ZZ") não entra: aparece numa linha separada. */
object Ufs {
    const val COLUMNS = 6
    const val ROWS = 8

    val all: List<Uf> = listOf(
        Uf("RR", "Roraima", "Em", row = 0, column = 1),
        Uf("AP", "Amapá", "No", row = 0, column = 2),
        Uf("AM", "Amazonas", "No", row = 1, column = 1),
        Uf("PA", "Pará", "No", row = 1, column = 2),
        Uf("MA", "Maranhão", "No", row = 1, column = 3),
        Uf("CE", "Ceará", "No", row = 1, column = 4),
        Uf("RN", "Rio Grande do Norte", "No", row = 1, column = 5),
        Uf("AC", "Acre", "No", row = 2, column = 0),
        Uf("RO", "Rondônia", "Em", row = 2, column = 1),
        Uf("TO", "Tocantins", "No", row = 2, column = 2),
        Uf("PI", "Piauí", "No", row = 2, column = 3),
        Uf("PE", "Pernambuco", "Em", row = 2, column = 4),
        Uf("PB", "Paraíba", "Na", row = 2, column = 5),
        Uf("MT", "Mato Grosso", "Em", row = 3, column = 1),
        Uf("GO", "Goiás", "Em", row = 3, column = 2),
        Uf("BA", "Bahia", "Na", row = 3, column = 3),
        Uf("SE", "Sergipe", "Em", row = 3, column = 4),
        Uf("AL", "Alagoas", "Em", row = 3, column = 5),
        Uf("MS", "Mato Grosso do Sul", "Em", row = 4, column = 1),
        Uf("DF", "Distrito Federal", "No", row = 4, column = 2),
        Uf("MG", "Minas Gerais", "Em", row = 4, column = 3),
        Uf("ES", "Espírito Santo", "No", row = 4, column = 4),
        Uf("PR", "Paraná", "No", row = 5, column = 1),
        Uf("SP", "São Paulo", "Em", row = 5, column = 2),
        Uf("RJ", "Rio de Janeiro", "No", row = 5, column = 3),
        Uf("SC", "Santa Catarina", "Em", row = 6, column = 1),
        Uf("RS", "Rio Grande do Sul", "No", row = 7, column = 1),
    )

    private val byCode: Map<String, Uf> = all.associateBy { it.code }

    fun byCode(code: String): Uf? = byCode[code.uppercase()]
}
