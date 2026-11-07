package com.puciec.eleicoes.android

import android.app.Application

class EleicoesApplication : Application() {
    /** Um container por processo: as telas pegam daqui os repositórios. */
    val container: AppContainer by lazy { AppContainer(this) }
}
