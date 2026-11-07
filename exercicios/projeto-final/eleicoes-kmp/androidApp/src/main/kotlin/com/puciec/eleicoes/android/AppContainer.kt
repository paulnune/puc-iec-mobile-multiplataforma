package com.puciec.eleicoes.android

import android.content.Context
import com.puciec.eleicoes.ApiConfig
import com.puciec.eleicoes.android.ui.theme.ThemePreference
import com.puciec.eleicoes.data.repository.CandidatesRepository
import com.puciec.eleicoes.data.repository.PartiesRepository
import com.puciec.eleicoes.data.repository.ResultsRepository
import com.puciec.eleicoes.data.source.EleicoesDataSource
import com.puciec.eleicoes.data.source.createEleicoesDataSource
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.favorites.InMemoryFavoritesRepository
import com.puciec.eleicoes.domain.model.DataStatus
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

    /** Porta de dados, via MCP. O #12 embrulha esta instância com o cache em disco. */
    val dataSource: EleicoesDataSource = createEleicoesDataSource()

    val partiesRepository = PartiesRepository(dataSource)
    val candidatesRepository = CandidatesRepository(dataSource, partiesRepository)
    val resultsRepository = ResultsRepository(dataSource)

    /** Em memória: some ao fechar o app. O #5 troca pela implementação com DataStore. */
    val favoritesRepository: FavoritesRepository = InMemoryFavoritesRepository()

    /** Em memória. O #13 grava a escolha no aparelho e põe a troca no menu da barra superior. */
    val themePreference = MutableStateFlow(ThemePreference.SYSTEM)

    /** De onde vêm os dados na tela. O #12 publica [DataStatus.Cached]; a faixa global mostra o aviso. */
    val dataStatus: StateFlow<DataStatus> = MutableStateFlow(DataStatus.Live)
}
