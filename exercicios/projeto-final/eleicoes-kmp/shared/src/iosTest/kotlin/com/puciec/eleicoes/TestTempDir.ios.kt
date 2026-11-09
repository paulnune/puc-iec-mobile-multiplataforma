package com.puciec.eleicoes

import platform.Foundation.NSTemporaryDirectory

actual fun testTempDirPath(): String = NSTemporaryDirectory().trimEnd('/')
