package com.puciec.eleicoes

import androidx.compose.ui.window.ComposeUIViewController
import com.puciec.eleicoes.ui.AppContainer
import com.puciec.eleicoes.ui.EleicoesApp
import platform.UIKit.UIViewController

/** Um container por processo, como o `EleicoesApplication` faz no Android. */
private val container: AppContainer by lazy { AppContainer() }

/** Chamado pelo Swift (`MainViewControllerKt.MainViewController()`): hospeda a UI Compose. */
fun MainViewController(): UIViewController = ComposeUIViewController { EleicoesApp(container) }
