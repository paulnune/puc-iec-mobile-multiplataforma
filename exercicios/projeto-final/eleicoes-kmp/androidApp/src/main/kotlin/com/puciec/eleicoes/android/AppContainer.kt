package com.puciec.eleicoes.android

import android.content.Context
import com.puciec.eleicoes.ApiConfig
import com.puciec.eleicoes.android.feature.colinha.DataStoreFavoritesRepository
import com.puciec.eleicoes.android.ui.theme.ThemePreference
import com.puciec.eleicoes.android.ui.theme.ThemeStore
import com.puciec.eleicoes.data.cache.CachingEleicoesDataSource
import com.puciec.eleicoes.data.cache.FileCacheStore
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.repository.EvolucaoRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.repository.ResultsRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.data.source.createEleicoesDataSource
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.model.DataStatus
import java.io.File
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
class AppContainer(context: Context) {
    /** Para DataStore e arquivos (#5, #12, #13). */
    val appContext: Context = context.applicationContext

    val hasApiKey: Boolean = ApiConfig.hasApiKey

    /**
     * Cache offline (#12): guarda em disco a última resposta boa de cada consulta e a devolve
     * quando a rede ou a API falham; reaproveita a resposta por 5 minutos (limite de 60 req/min).
     */
    private val cachingSource = CachingEleicoesDataSource(
        delegate = createEleicoesDataSource(),
        store = FileCacheStore(File(appContext.filesDir, "eleicoes-cache")),
        now = System::currentTimeMillis,
    )

    /** Porta de dados, via MCP, embrulhada pelo cache em disco. */
    val dataSource: EleicoesDataSource = cachingSource

    val partiesRepository = PartiesRepository(dataSource)
    val candidatesRepository = CandidatesRepository(dataSource, partiesRepository)
    val resultsRepository = ResultsRepository(dataSource)
    val evolucaoRepository = EvolucaoRepository(candidatesRepository)

    /** Colinha gravada no aparelho com DataStore (#5): sobrevive a fechar o app. */
    val favoritesRepository: FavoritesRepository = DataStoreFavoritesRepository(appContext)

    /** Escopo dos processos de fundo do container (gravar a escolha de tema). Vive com o processo. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Tema escolhido, lido do aparelho. Começa em [ThemePreference.SYSTEM] até o DataStore responder. */
    private val themeStore = ThemeStore(appContext)
    val themePreference: StateFlow<ThemePreference> = themeStore.preference
        .stateIn(appScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)

    /** Troca o tema na tela e grava no aparelho. */
    fun setThemePreference(preference: ThemePreference) {
        appScope.launch { themeStore.save(preference) }
    }

    /** De onde vêm os dados na tela. O cache (#12) publica [DataStatus.Cached]; a faixa global mostra o aviso. */
    val dataStatus: StateFlow<DataStatus> = cachingSource.status
}
