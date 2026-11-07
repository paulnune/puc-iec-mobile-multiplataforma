package com.puciec.eleicoes.domain.model

/**
 * Cargo, com o valor que a API usa no campo `office`.
 *
 * [urnaOrder] e [digits] valem para a colinha (#5): a urna de 2026 pede, nesta ordem,
 * Deputado Federal (4 dígitos), Deputado Estadual ou Distrital (5), Senador (3, duas vagas),
 * Governador (2) e Presidente (2). Vices e suplentes não são votados em separado.
 */
enum class Office(
    val apiValue: String,
    val label: String,
    val urnaOrder: Int?,
    val digits: Int?,
    val colinhaSlots: Int,
) {
    FEDERAL_DEPUTY("federalDeputy", "Deputado Federal", urnaOrder = 1, digits = 4, colinhaSlots = 1),
    STATE_DEPUTY("stateDeputy", "Deputado Estadual", urnaOrder = 2, digits = 5, colinhaSlots = 1),
    DISTRICT_DEPUTY("districtDeputy", "Deputado Distrital", urnaOrder = 2, digits = 5, colinhaSlots = 1),
    SENATOR("senator", "Senador", urnaOrder = 3, digits = 3, colinhaSlots = 2),
    GOVERNOR("governor", "Governador", urnaOrder = 4, digits = 2, colinhaSlots = 1),
    PRESIDENT("president", "Presidente", urnaOrder = 5, digits = 2, colinhaSlots = 1),
    VICE_PRESIDENT("vicePresident", "Vice-presidente", urnaOrder = null, digits = null, colinhaSlots = 0),
    VICE_GOVERNOR("viceGovernor", "Vice-governador", urnaOrder = null, digits = null, colinhaSlots = 0),
    SENATOR_FIRST_ALTERNATE("senatorFirstAlternate", "1º suplente de senador", urnaOrder = null, digits = null, colinhaSlots = 0),
    SENATOR_SECOND_ALTERNATE("senatorSecondAlternate", "2º suplente de senador", urnaOrder = null, digits = null, colinhaSlots = 0),
    UNKNOWN("", "Outro cargo", urnaOrder = null, digits = null, colinhaSlots = 0);

    /** Cargo que aparece na urna (e por isso pode entrar na colinha). */
    val isOnBallot: Boolean get() = urnaOrder != null

    companion object {
        fun fromApi(value: String?): Office = entries.firstOrNull { it.apiValue == value } ?: UNKNOWN
    }
}
