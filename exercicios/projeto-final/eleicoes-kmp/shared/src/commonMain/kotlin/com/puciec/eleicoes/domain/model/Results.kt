package com.puciec.eleicoes.domain.model

/** Abrangência de um resultado: Brasil, UF ou município. */
enum class PlaceScope(val apiValue: String) {
    BRASIL("br"),
    UF("uf"),
    MUNICIPIO("mu"),
}

data class VoteCount(
    val ballotName: String?,
    val partyAcronym: String?,
    val votes: Long,
    val voteShare: Double?,
)

data class PartyVotes(val number: Int, val acronym: String, val votes: Long)

/** Totais de um cargo num local (só há dados do 1º turno). */
data class PlaceResult(
    val office: Office,
    val scope: PlaceScope,
    val state: String,
    val code: String,
    val name: String?,
    val round: Int,
    val final: Boolean,
    val countedShare: Double?,
    val electorate: Long?,
    val turnout: Long?,
    val abstention: Long?,
    val validVotes: Long?,
    val blankVotes: Long?,
    val nullVotes: Long?,
    val leader: VoteCount?,
    val runnerUp: VoteCount?,
    val marginVotes: Long?,
    val marginShare: Double?,
    val votes: List<VoteCount>,
    val parties: List<PartyVotes>,
)

data class Party(val id: String, val number: Int, val acronym: String, val name: String)

/** Ordenação da tool `list_wealth_changes` (quebrada no servidor em 07/11; ver #7). */
enum class WealthSort(val apiValue: String) {
    ABS("abs"),
    GAIN("gain"),
    DROP("drop"),
}

data class WealthChange(
    val candidateId: String,
    val ballotName: String,
    val officeLabel: String,
    val state: String,
    val partyAcronym: String?,
    val photoUrl: String?,
    val currentAssets: Double,
    val priorAssets: Double,
    val delta: Double,
    val ratio: Double?,
    val priorYear: Int,
    val priorOfficeLabel: String,
)

/** Uma página de resultados. [totalPages] nulo significa "a API não informou". */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int?,
    val total: Int?,
) {
    val hasNext: Boolean get() = totalPages != null && page < totalPages
}

/** De onde veio o dado que está na tela. O cache em disco (#12) publica [Cached]. */
sealed interface DataStatus {
    data object Live : DataStatus
    data class Cached(val savedAtEpochMillis: Long) : DataStatus
}
