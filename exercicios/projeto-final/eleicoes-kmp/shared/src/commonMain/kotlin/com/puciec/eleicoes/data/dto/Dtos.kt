package com.puciec.eleicoes.data.dto

import kotlinx.serialization.Serializable

// Formato das respostas da API (iguais na REST e no MCP). Campos que o app não usa ficam de fora:
// o Json ignora chaves desconhecidas. Tudo tem valor padrão para tolerar nulos e ausências.

@Serializable
data class PaginationDto(
    val page: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,
    val totalPages: Int? = null,
)

@Serializable
data class PageDto<T>(
    val data: List<T> = emptyList(),
    val pagination: PaginationDto? = null,
)

@Serializable
data class ResultDto(
    val round: Int? = null,
    val status: String? = null,
    val outcome: String? = null,
    val votes: Long? = null,
    val voteShare: Double? = null,
    val final: Boolean? = null,
)

@Serializable
data class CandidateDto(
    val id: String,
    val partyId: String = "",
    val ballotName: String = "",
    val fullName: String = "",
    val number: Int = 0,
    val office: String? = null,
    val officeLabel: String? = null,
    val state: String = "",
    val birthDate: String? = null,
    val birthState: String? = null,
    val birthCity: String? = null,
    val gender: String? = null,
    val education: String? = null,
    val occupation: String? = null,
    val nationality: String? = null,
    val totalAssets: Double = 0.0,
    val assetsDisclosed: Boolean = false,
    val photoUrl: String? = null,
    val result: ResultDto? = null,
)

@Serializable
data class AssetDto(
    val id: String,
    val order: Int = 0,
    val typeLabel: String? = null,
    val description: String? = null,
    val value: Double = 0.0,
)

@Serializable
data class FilingDto(
    val id: String,
    val partyId: String = "",
    val ballotName: String = "",
    val number: Int = 0,
    val office: String? = null,
    val officeLabel: String? = null,
    val state: String = "",
    val totalAssets: Double = 0.0,
    val assetsDisclosed: Boolean = false,
    val result: ResultDto? = null,
    val year: Int = 0,
    val electionName: String? = null,
)

@Serializable
data class WealthChangeDto(
    val id: String,
    val ballotName: String = "",
    val officeLabel: String = "",
    val state: String = "",
    val partyId: String = "",
    val photoUrl: String? = null,
    val currentAssets: Double = 0.0,
    val priorAssets: Double = 0.0,
    val delta: Double = 0.0,
    val ratio: Double? = null,
    val priorYear: Int = 0,
    val priorOfficeLabel: String = "",
)

@Serializable
data class VoteDto(
    val votes: Long = 0,
    val voteShare: Double? = null,
    val ballotName: String? = null,
    val partyAcronym: String? = null,
)

@Serializable
data class PartyVotesDto(
    val number: Int = 0,
    val acronym: String = "",
    val votes: Long = 0,
)

@Serializable
data class PlaceResultDto(
    val office: String = "",
    val scope: String = "",
    val state: String = "",
    val code: String = "",
    val name: String? = null,
    val round: Int = 1,
    val final: Boolean = false,
    val countedShare: Double? = null,
    val electorate: Long? = null,
    val turnout: Long? = null,
    val abstention: Long? = null,
    val validVotes: Long? = null,
    val blankVotes: Long? = null,
    val nullVotes: Long? = null,
    val leader: VoteDto? = null,
    val runnerUp: VoteDto? = null,
    val marginVotes: Long? = null,
    val marginShare: Double? = null,
    val votes: List<VoteDto> = emptyList(),
    val parties: List<PartyVotesDto> = emptyList(),
)

@Serializable
data class PartyDto(
    val id: String,
    val number: Int = 0,
    val acronym: String = "",
    val name: String = "",
)
