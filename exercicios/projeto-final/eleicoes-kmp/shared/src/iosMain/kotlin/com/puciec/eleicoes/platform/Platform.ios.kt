package com.puciec.eleicoes.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
actual fun appFilesDirPath(): String {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        .first() as String
    // Application Support não existe até alguém criar.
    NSFileManager.defaultManager.createDirectoryAtPath(base, withIntermediateDirectories = true, attributes = null, error = null)
    return base
}

@OptIn(ExperimentalForeignApi::class)
actual fun sha256Hex(text: String): String {
    val data = text.encodeToByteArray()
    // addressOf(0) falha em array vazio: com texto vazio o buffer é um byte de enchimento e o tamanho passado é 0.
    val input = if (data.isEmpty()) ByteArray(1) else data
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    input.usePinned { src ->
        digest.usePinned { out ->
            CC_SHA256(src.addressOf(0), data.size.convert(), out.addressOf(0))
        }
    }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}

actual fun formatDayMonthTime(epochMillis: Long): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd/MM 'às' HH:mm"
        locale = NSLocale("pt_BR")
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}

@Composable
actual fun rememberShareText(): (String) -> Unit = remember {
    { text ->
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        topViewController()?.presentViewController(sheet, animated = true, completion = null)
    }
}

@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) = Unit

private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
