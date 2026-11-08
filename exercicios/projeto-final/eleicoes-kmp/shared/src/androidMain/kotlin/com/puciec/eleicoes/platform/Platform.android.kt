package com.puciec.eleicoes.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Preenchido por `EleicoesApplication.onCreate`, antes de qualquer uso das funções de plataforma. */
object AndroidContext {
    lateinit var app: Application
}

actual fun appFilesDirPath(): String = AndroidContext.app.filesDir.absolutePath

actual fun sha256Hex(text: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(text.encodeToByteArray())
        .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

actual fun formatDayMonthTime(epochMillis: Long): String =
    SimpleDateFormat("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(epochMillis))

@Composable
actual fun rememberShareText(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, "Compartilhar colinha"))
        }
    }
}

@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
