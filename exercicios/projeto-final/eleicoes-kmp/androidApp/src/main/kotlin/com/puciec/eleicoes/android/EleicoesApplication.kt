package com.puciec.eleicoes.android

import android.app.Application
import com.puciec.eleicoes.platform.AndroidContext

class EleicoesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Antes de qualquer uso de appFilesDirPath(): o container só nasce quando a tela pede.
        AndroidContext.app = this
    }

    /** Um container por processo: as telas pegam daqui os repositórios. */
    val container: AppContainer by lazy { AppContainer() }
}
