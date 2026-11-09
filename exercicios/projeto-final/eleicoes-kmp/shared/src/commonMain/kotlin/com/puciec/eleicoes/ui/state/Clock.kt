package com.puciec.eleicoes.ui.state

import kotlin.time.Clock

/** Relógio multiplataforma: milissegundos desde 1970 (substitui System.currentTimeMillis). */
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
