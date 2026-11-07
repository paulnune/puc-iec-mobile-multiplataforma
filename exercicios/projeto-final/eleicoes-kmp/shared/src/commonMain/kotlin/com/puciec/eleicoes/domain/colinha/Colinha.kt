package com.puciec.eleicoes.domain.colinha

import com.puciec.eleicoes.domain.favorites.FavoriteCandidate
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.format.toTitleCase
import com.puciec.eleicoes.domain.model.Office
import com.puciec.eleicoes.domain.model.Outcome
import kotlinx.coroutines.flow.first

/**
 * Regras da colinha (#5): a cola com os números dos seus candidatos, na ordem da urna.
 *
 * - Só entram cargos votados na urna ([Office.isOnBallot]).
 * - Cada cargo tem [Office.colinhaSlots] vagas: 1, ou 2 para senador.
 * - Os cargos estaduais (deputados, senador, governador) são todos da mesma UF, porque o eleitor vota numa UF só.
 *   Presidente vale para o Brasil inteiro e não entra nessa regra.
 */
object Colinha {

    /** Cargo que depende da UF do eleitor. Presidente é nacional. */
    val Office.isStateOffice: Boolean
        get() = isOnBallot && this != Office.PRESIDENT

    /**
     * Ordem da urna: Dep. Federal → Dep. Estadual/Distrital → Senador (1ª e 2ª vaga) → Governador → Presidente.
     * No mesmo cargo, vale a ordem em que foram favoritados (o 1º senador é a 1ª vaga).
     */
    fun urnaOrder(favorites: List<FavoriteCandidate>): List<FavoriteCandidate> =
        favorites
            .filter { it.office.isOnBallot }
            .sortedBy { it.office.urnaOrder } // sortedBy é estável: mantém a ordem de inclusão dentro do cargo.

    /** UF da colinha: a dos cargos estaduais já favoritados, ou nula se só há presidente (ou nada). */
    fun stateOf(favorites: List<FavoriteCandidate>): String? =
        favorites.firstOrNull { it.office.isStateOffice }?.state

    /** O que acontece se o usuário favoritar [candidate] com a colinha [current]. */
    fun evaluate(current: List<FavoriteCandidate>, candidate: FavoriteCandidate): AddDecision {
        if (current.any { it.id == candidate.id }) return AddDecision.AlreadyIn
        if (!candidate.office.isOnBallot) return AddDecision.NotOnBallot

        if (candidate.office.isStateOffice) {
            val otherState = current.filter { it.office.isStateOffice && it.state != candidate.state }
            if (otherState.isNotEmpty()) {
                return AddDecision.OtherState(
                    currentState = otherState.first().state,
                    newState = candidate.state,
                    toRemove = otherState,
                )
            }
        }

        val occupants = current.filter { it.office.sameSeatAs(candidate.office) }
        if (occupants.size >= candidate.office.colinhaSlots) return AddDecision.SeatFull(occupants)

        return AddDecision.Add
    }

    /** Lista de [current] depois de trocar [toRemove] por [candidate]. A ordem de inclusão é mantida. */
    fun replace(
        current: List<FavoriteCandidate>,
        toRemove: Collection<FavoriteCandidate>,
        candidate: FavoriteCandidate,
    ): List<FavoriteCandidate> {
        val removeIds = toRemove.map { it.id }.toSet() + candidate.id
        return current.filterNot { it.id in removeIds } + candidate
    }

    /** Nome do lugar na urna: "Senador · 1ª vaga" para senadores, o cargo para os demais. */
    fun seatLabel(favorite: FavoriteCandidate, ordered: List<FavoriteCandidate>): String {
        val base = favorite.office.label
        if (favorite.office.colinhaSlots <= 1) return base
        val position = ordered.filter { it.office.sameSeatAs(favorite.office) }.indexOfFirst { it.id == favorite.id }
        return if (position < 0) base else "$base · ${position + 1}ª vaga"
    }

    /** Número com os dígitos do cargo, completando com zeros à esquerda: deputado federal 4, senador 3... */
    fun urnaDigits(favorite: FavoriteCandidate): String {
        val digits = favorite.office.digits ?: return favorite.number.toString()
        return favorite.number.toString().padStart(digits, '0')
    }

    /** Placar da colinha: a eleição já aconteceu, então dá para contar quantos se elegeram. */
    fun scoreboard(favorites: List<FavoriteCandidate>): Scoreboard {
        val onBallot = favorites.filter { it.office.isOnBallot }
        return Scoreboard(
            total = onBallot.size,
            elected = onBallot.count { it.outcome == Outcome.ELECTED },
            runoff = onBallot.count { it.outcome == Outcome.RUNOFF },
        )
    }

    /** Texto para compartilhar: uma linha por voto, na ordem da urna. */
    fun shareText(favorites: List<FavoriteCandidate>): String {
        val ordered = urnaOrder(favorites)
        val state = stateOf(ordered)
        val header = "Minha colinha · Eleições 2026" + (state?.let { " ($it)" } ?: "")
        val lines = ordered.map { favorite ->
            val party = favorite.partyAcronym?.let { " ($it)" }.orEmpty()
            val outcome = favorite.outcome.takeIf { it != Outcome.UNKNOWN }?.let { " · ${it.label}" }.orEmpty()
            "${seatLabel(favorite, ordered)}: ${urnaDigits(favorite)} · ${favorite.ballotName.toTitleCase()}$party$outcome"
        }
        val score = scoreboard(ordered)
        val footer = "Placar: ${score.elected} de ${score.total} eleitos" +
            if (score.runoff > 0) ", ${score.runoff} no 2º turno" else ""
        return (listOf(header, "") + lines + listOf("", footer)).joinToString("\n")
    }

    /** Estadual e distrital ocupam o mesmo lugar na urna (5 dígitos, a 2ª tela). */
    private fun Office.sameSeatAs(other: Office): Boolean = urnaOrder != null && urnaOrder == other.urnaOrder
}

/** Resultado de [Colinha.evaluate]. */
sealed interface AddDecision {
    /** Cabe: favorita direto. */
    data object Add : AddDecision

    /** Já está na colinha. */
    data object AlreadyIn : AddDecision

    /** Vice e suplentes não são votados em separado: não vão para a cola. */
    data object NotOnBallot : AddDecision

    /** O cargo já está cheio: pergunta qual dos [occupants] sai. */
    data class SeatFull(val occupants: List<FavoriteCandidate>) : AddDecision

    /** A colinha é de outra UF: trocar de UF tira todos os cargos estaduais de [currentState]. */
    data class OtherState(
        val currentState: String,
        val newState: String,
        val toRemove: List<FavoriteCandidate>,
    ) : AddDecision
}

data class Scoreboard(val total: Int, val elected: Int, val runoff: Int)

/** Troca [toRemove] por [candidate] na colinha. */
suspend fun FavoritesRepository.replace(toRemove: Collection<FavoriteCandidate>, candidate: FavoriteCandidate) {
    toRemove.forEach { remove(it.id) }
    add(candidate)
}

/**
 * ★ do detalhe com as regras da colinha: tira se já estava; favorita se cabe.
 * Devolve a [AddDecision] quando precisa perguntar ao usuário (cargo cheio, outra UF, fora da urna), ou nulo se já resolveu.
 */
suspend fun FavoritesRepository.toggleInColinha(candidate: FavoriteCandidate): AddDecision? {
    val current = favorites.first()
    if (current.any { it.id == candidate.id }) {
        remove(candidate.id)
        return null
    }
    return when (val decision = Colinha.evaluate(current, candidate)) {
        AddDecision.Add -> {
            add(candidate)
            null
        }
        AddDecision.AlreadyIn -> null
        else -> decision
    }
}
