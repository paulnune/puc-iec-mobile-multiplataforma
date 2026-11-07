package com.puciec.eleicoes.domain.mapa

import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.PlaceResult
import com.puciec.eleicoes.domain.model.VoteCount

/** Um item da legenda: o partido e o primeiro líder dele na lista ("Flavio Bolsonaro · PL"). */
data class LegendItem(val partyAcronym: String, val leaderName: String)

/**
 * O que a aba Mapa (#6) mostra para um cargo.
 *
 * [headline] é a frase nacional (Presidente) ou a dica de toque (Governador); [states] são as UFs com
 * líder, pela ordem dos votos do líder; [exterior] só existe para Presidente; [noData] são as UFs que
 * vieram sem líder.
 */
data class MapaOverview(
    val headline: String,
    val states: List<PlaceResult>,
    val exterior: PlaceResult?,
    val noData: List<String>,
    val legend: List<LegendItem>,
) {
    val isEmpty: Boolean get() = states.isEmpty() && noData.isEmpty() && exterior == null
}

private const val EXTERIOR = "ZZ"

fun buildMapaOverview(office: Office, national: PlaceResult?, places: List<PlaceResult>): MapaOverview {
    val ufs = places.filter { Ufs.byCode(it.code) != null }
    val states = ufs.filter { it.leader != null }.sortedByDescending { it.leader?.votes ?: 0 }
    val exterior = if (office == Office.PRESIDENT) {
        places.firstOrNull { it.code == EXTERIOR && it.leader != null }
    } else {
        null
    }
    val headline = national
        ?.takeIf { office == Office.PRESIDENT && it.leader != null }
        ?.let(::placeSentence)
        ?: GOVERNOR_HINT
    val legend = states
        .mapNotNull { place ->
            val leader = place.leader ?: return@mapNotNull null
            val party = leader.partyAcronym?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            LegendItem(party, leader.ballotName.orEmpty().toNameProse())
        }
        .distinctBy { it.partyAcronym }
    return MapaOverview(
        headline = headline,
        states = states,
        exterior = exterior,
        noData = ufs.filter { it.leader == null }.map { it.code },
        legend = legend,
    )
}

/** Votos de cada candidato no lugar, do mais votado para o menos votado. */
fun candidateRows(place: PlaceResult): List<VoteCount> = place.votes.sortedByDescending { it.votes }
