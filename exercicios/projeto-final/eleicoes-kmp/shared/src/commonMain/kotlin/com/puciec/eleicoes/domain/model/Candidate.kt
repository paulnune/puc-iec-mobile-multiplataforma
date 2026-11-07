package com.puciec.eleicoes.domain.model

/** Desfecho da candidatura, como a API devolve em `result.outcome`. */
enum class Outcome(val apiValue: String?, val label: String) {
    ELECTED("elected", "Eleito"),
    RUNOFF("runoff", "2º turno"),
    NOT_ELECTED("notElected", "Não eleito"),
    ALTERNATE("alternate", "Suplente"),
    UNKNOWN(null, "Sem resultado");

    companion object {
        fun fromApi(value: String?): Outcome = entries.firstOrNull { it.apiValue != null && it.apiValue == value } ?: UNKNOWN
    }
}

data class CandidateResult(
    val round: Int,
    val outcome: Outcome,
    val votes: Long?,
    val voteShare: Double?,
    val final: Boolean,
)

/** Uma candidatura de 2026. Nomes vêm em maiúsculas da API; a UI formata. */
data class Candidate(
    val id: String,
    val ballotName: String,
    val fullName: String,
    val number: Int,
    val office: Office,
    val officeLabel: String,
    val state: String,
    val partyId: String,
    val partyAcronym: String?,
    val photoUrl: String?,
    val totalAssets: Double,
    val assetsDisclosed: Boolean,
    val gender: String?,
    val birthDate: String?,
    val birthCity: String?,
    val birthState: String?,
    val nationality: String?,
    val education: String?,
    val occupation: String?,
    val result: CandidateResult?,
) {
    val outcome: Outcome get() = result?.outcome ?: Outcome.UNKNOWN

    /** Brasileiro naturalizado (nasceu no exterior; a API usa `birthState` = "ZZ"). */
    val isNaturalized: Boolean get() = nationality?.contains("NATURALIZ", ignoreCase = true) == true
}

/** Um bem declarado ao TSE. */
data class Asset(
    val id: String,
    val order: Int,
    val typeLabel: String,
    val description: String,
    val value: Double,
)

/** Uma candidatura da mesma pessoa em outra eleição (linha do tempo desde 2006). */
data class Filing(
    val candidateId: String,
    val year: Int,
    val electionName: String,
    val office: Office,
    val officeLabel: String,
    val state: String,
    val partyId: String,
    val partyAcronym: String?,
    val number: Int,
    val totalAssets: Double,
    val assetsDisclosed: Boolean,
    val outcome: Outcome,
)
