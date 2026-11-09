package com.puciec.eleicoes.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.puciec.eleicoes.ui.EleicoesApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Antes do super: a splash some quando o primeiro frame do Compose estiver pronto.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as EleicoesApplication).container
        setContent { EleicoesApp(container) }
    }
}
