package com.puciec.eleicoes.platform

import androidx.compose.runtime.Composable

/** Pasta de arquivos do app, que o sistema não limpa sozinho (Android: filesDir; iOS: Application Support). */
expect fun appFilesDirPath(): String

/** SHA-256 de [text] em hexadecimal minúsculo, 2 dígitos por byte. */
expect fun sha256Hex(text: String): String

/** "dd/MM 'às' HH:mm" em pt-BR, no fuso do aparelho, independente do idioma dele. */
expect fun formatDayMonthTime(epochMillis: Long): String

/** Devolve a ação que abre a folha de compartilhar do sistema com o texto dado. */
@Composable
expect fun rememberShareText(): (String) -> Unit

/** Faz os ícones das barras do sistema acompanharem o tema do app (no iOS não faz nada). */
@Composable
expect fun SystemBarsEffect(darkTheme: Boolean)
