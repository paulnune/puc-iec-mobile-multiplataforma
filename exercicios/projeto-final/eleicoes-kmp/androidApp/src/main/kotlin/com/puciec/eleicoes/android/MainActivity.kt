package com.puciec.eleicoes.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO(#13): installSplashScreen() aqui, antes do super.onCreate.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as EleicoesApplication).container
        setContent { EleicoesApp(container) }
    }
}
