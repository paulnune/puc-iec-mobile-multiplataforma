package com.puciec.eleicoes

import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceResult
import com.puciec.eleicoes.domain.model.PlaceScope
import com.puciec.eleicoes.domain.model.VoteCount

/** Monta resultados do Mapa (#6) com só os campos que importam no teste. */
internal fun vote(name: String, party: String, votes: Long, share: Double? = null) =
    VoteCount(ballotName = name, partyAcronym = party, votes = votes, voteShare = share)

internal fun placeOf(
    code: String,
    office: Office = Office.PRESIDENT,
    scope: PlaceScope = if (code == "BR") PlaceScope.BRASIL else PlaceScope.UF,
    leader: VoteCount? = null,
    runnerUp: VoteCount? = null,
    countedShare: Double? = 100.0,
    votes: List<VoteCount> = emptyList(),
) = PlaceResult(
    office = office,
    scope = scope,
    state = code,
    code = code,
    name = null,
    round = 1,
    final = true,
    countedShare = countedShare,
    electorate = null,
    turnout = null,
    abstention = null,
    validVotes = null,
    blankVotes = null,
    nullVotes = null,
    leader = leader,
    runnerUp = runnerUp,
    marginVotes = null,
    marginShare = null,
    votes = votes,
    parties = emptyList(),
)
