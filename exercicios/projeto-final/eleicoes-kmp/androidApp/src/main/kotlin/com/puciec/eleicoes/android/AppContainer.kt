package com.puciec.eleicoes.android

import com.puciec.eleicoes.ApiConfig
import com.puciec.eleicoes.android.feature.colinha.DataStoreFavoritesRepository
import com.puciec.eleicoes.android.ui.theme.ThemePreference
import com.puciec.eleicoes.android.ui.theme.ThemeStore
import com.puciec.eleicoes.android.ui.state.nowMillis
import com.puciec.eleicoes.data.cache.CachingEleicoesDataSource
import com.puciec.eleicoes.data.cache.FileCacheStore
import com.puciec.eleicoes.data.store.createPrefsDataStore
import com.puciec.eleicoes.platform.appFilesDirPath
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.repository.EvolucaoRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.repository.ResultsRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.data.source.createEleicoesDataSource
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.model.DataStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Injeção de dependência manual: um lugar só cria as peças e as entrega às telas.
 * Cada issue troca a sua peça aqui, sem mexer nas telas dos outros.
 */
class AppContainer {
    /** Pasta do cache offline do app inteiro (#12); o catálogo de partidos tem a sua, separada. */
    val cacheDirPath: String = appFilesDirPath() + "/eleicoes-cache"

    val hasApiKey: Boolean = ApiConfig.hasApiKey

    /**
     * Cache offline (#12): guarda em disco a última resposta boa de cada consulta e a devolve
     * quando a rede ou a API falham; reaproveita a resposta por 5 minutos (limite de 60 req/min).
     */
    private val cachingSource = CachingEleicoesDataSource(
        delegate = createEleicoesDataSource(),
        store = FileCacheStore(cacheDirPath),
        now = ::nowMillis,
    )

    /** Porta de dados, via MCP, embrulhada pelo cache em disco. */
    val dataSource: EleicoesDataSource = cachingSource

    val partiesRepository = PartiesRepository(dataSource)
    val candidatesRepository = CandidatesRepository(dataSource, partiesRepository)
    val resultsRepository = ResultsRepository(dataSource)
    val evolucaoRepository = EvolucaoRepository(candidatesRepository)

    /** Colinha gravada no aparelho com DataStore (#5): sobrevive a fechar o app. */
    val favoritesRepository: FavoritesRepository = DataStoreFavoritesRepository(createPrefsDataStore("colinha.preferences_pb"))

    /** Escopo dos processos de fundo do container (gravar a escolha de tema). Vive com o processo. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Tema escolhido, lido do aparelho. Começa em [ThemePreference.SYSTEM] até o DataStore responder. */
    private val themeStore = ThemeStore(createPrefsDataStore("tema.preferences_pb"))
    val themePreference: StateFlow<ThemePreference> = themeStore.preference
        .stateIn(appScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)

    /** Troca o tema na tela e grava no aparelho. */
    fun setThemePreference(preference: ThemePreference) {
        appScope.launch { themeStore.save(preference) }
    }

    /** De onde vêm os dados na tela. O cache (#12) publica [DataStatus.Cached]; a faixa global mostra o aviso. */
    val dataStatus: StateFlow<DataStatus> = cachingSource.status
}
