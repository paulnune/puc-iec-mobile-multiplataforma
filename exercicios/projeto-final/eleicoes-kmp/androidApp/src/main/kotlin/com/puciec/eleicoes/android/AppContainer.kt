package com.puciec.eleicoes.android

import android.content.Context
import com.puciec.eleicoes.ApiConfig
import com.puciec.eleicoes.android.ui.theme.ThemePreference
import com.puciec.eleicoes.data.cache.CachingEleicoesDataSource
import com.puciec.eleicoes.data.cache.FileCacheStore
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.repository.ResultsRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.data.source.createEleicoesDataSource
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.favorites.InMemoryFavoritesRepository
import com.puciec.eleicoes.domain.model.DataStatus
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    /** Em memória: some ao fechar o app. O #5 troca pela implementação com DataStore. */
    val favoritesRepository: FavoritesRepository = InMemoryFavoritesRepository()

    /** Em memória. O #13 grava a escolha no aparelho e põe a troca no menu da barra superior. */
    val themePreference = MutableStateFlow(ThemePreference.SYSTEM)

    /** De onde vêm os dados na tela. O cache (#12) publica [DataStatus.Cached]; a faixa global mostra o aviso. */
    val dataStatus: StateFlow<DataStatus> = cachingSource.status
}
