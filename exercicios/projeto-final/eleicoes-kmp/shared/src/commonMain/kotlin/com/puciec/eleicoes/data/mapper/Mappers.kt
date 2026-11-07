package com.puciec.eleicoes.data.mapper

import com.puciec.eleicoes.data.dto.AssetDto
import com.puciec.eleicoes.data.dto.CandidateDto
import com.puciec.eleicoes.data.dto.FilingDto
import com.puciec.eleicoes.data.dto.PageDto
import com.puciec.eleicoes.data.dto.PartyDto
import com.puciec.eleicoes.data.dto.PlaceResultDto
import com.puciec.eleicoes.data.dto.ResultDto
import com.puciec.eleicoes.data.dto.VoteDto
import com.puciec.eleicoes.data.dto.WealthChangeDto
import com.puciec.eleicoes.domain.model.Asset
import com.puciec.eleicoes.domain.model.Candidate
import com.puciec.eleicoes.domain.model.CandidateResult
import com.puciec.eleicoes.domain.model.Filing
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Outcome
import com.puciec.eleicoes.domain.model.Page
import com.puciec.eleicoes.domain.model.Party
import com.puciec.eleicoes.domain.model.PartyVotes
import com.puciec.eleicoes.domain.model.PlaceResult
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.VoteCount
import com.puciec.eleicoes.domain.model.WealthChange

// DTO (formato da API) → modelo de domínio (o que as telas usam).
// [partyAcronyms]: sigla por id do partido; a candidatura só traz o partyId.

internal fun CandidateDto.toDomain(partyAcronyms: Map<String, String>): Candidate = Candidate(
    id = id,
    ballotName = ballotName,
    fullName = fullName,
    number = number,
    office = Office.fromApi(office),
    officeLabel = officeLabel.orEmpty(),
    state = state,
    partyId = partyId,
    partyAcronym = partyAcronyms[partyId],
    photoUrl = photoUrl,
    totalAssets = totalAssets,
    assetsDisclosed = assetsDisclosed,
    gender = gender,
    birthDate = birthDate,
    birthCity = birthCity,
    birthState = birthState,
    nationality = nationality,
    education = education,
    occupation = occupation,
    result = result?.toDomain(),
)

internal fun ResultDto.toDomain(): CandidateResult = CandidateResult(
    round = round ?: 1,
    outcome = Outcome.fromApi(outcome),
    votes = votes,
    voteShare = voteShare,
    final = final ?: false,
)

internal fun AssetDto.toDomain(): Asset = Asset(
    id = id,
    order = order,
    typeLabel = typeLabel.orEmpty(),
    description = description.orEmpty(),
    value = value,
)

internal fun FilingDto.toDomain(partyAcronyms: Map<String, String>): Filing = Filing(
    candidateId = id,
    year = year,
    electionName = electionName.orEmpty(),
    office = Office.fromApi(office),
    officeLabel = officeLabel.orEmpty(),
    state = state,
    partyId = partyId,
    partyAcronym = partyAcronyms[partyId],
    number = number,
    totalAssets = totalAssets,
    assetsDisclosed = assetsDisclosed,
    outcome = Outcome.fromApi(result?.outcome),
)

internal fun WealthChangeDto.toDomain(partyAcronyms: Map<String, String>): WealthChange = WealthChange(
    candidateId = id,
    ballotName = ballotName,
    officeLabel = officeLabel,
    state = state,
    partyAcronym = partyAcronyms[partyId],
    photoUrl = photoUrl,
    currentAssets = currentAssets,
    priorAssets = priorAssets,
    delta = delta,
    ratio = ratio,
    priorYear = priorYear,
    priorOfficeLabel = priorOfficeLabel,
)

internal fun VoteDto.toDomain(): VoteCount = VoteCount(
    ballotName = ballotName,
    partyAcronym = partyAcronym,
    votes = votes,
    voteShare = voteShare,
)

internal fun PlaceResultDto.toDomain(): PlaceResult = PlaceResult(
    office = Office.fromApi(office),
    scope = PlaceScope.entries.firstOrNull { it.apiValue == scope } ?: PlaceScope.UF,
    state = state,
    code = code,
    name = name,
    round = round,
    final = final,
    countedShare = countedShare,
    electorate = electorate,
    turnout = turnout,
    abstention = abstention,
    validVotes = validVotes,
    blankVotes = blankVotes,
    nullVotes = nullVotes,
    leader = leader?.toDomain(),
    runnerUp = runnerUp?.toDomain(),
    marginVotes = marginVotes,
    marginShare = marginShare,
    votes = votes.map { it.toDomain() },
    parties = parties.map { PartyVotes(number = it.number, acronym = it.acronym, votes = it.votes) },
)

internal fun PartyDto.toDomain(): Party = Party(id = id, number = number, acronym = acronym, name = name)

internal fun <D, T> PageDto<D>.toPage(requestedPage: Int, transform: (D) -> T): Page<T> = Page(
    items = data.map(transform),
    page = pagination?.page ?: requestedPage,
    totalPages = pagination?.totalPages,
    total = pagination?.total,
)
