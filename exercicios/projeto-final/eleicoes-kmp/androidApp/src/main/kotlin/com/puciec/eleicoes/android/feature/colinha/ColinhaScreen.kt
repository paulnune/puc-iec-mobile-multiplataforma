package com.puciec.eleicoes.android.feature.colinha

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puciec.eleicoes.android.AppContainer
import com.puciec.eleicoes.android.navigation.ColinhaRoute
import com.puciec.eleicoes.android.navigation.DetalheRoute
import com.puciec.eleicoes.android.ui.components.EmptyState
import com.puciec.eleicoes.domain.favorites.FavoriteCandidate
import com.puciec.eleicoes.domain.favorites.FavoritesRepository
import com.puciec.eleicoes.domain.format.toTitleCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

fun NavGraphBuilder.colinhaGraph(navController: NavController, container: AppContainer) {
    composable<ColinhaRoute> {
        val viewModel: ColinhaViewModel = viewModel(factory = ColinhaViewModel.factory(container))
        ColinhaScreen(viewModel, onCandidateClick = { navController.navigate(DetalheRoute(it.id)) })
    }
}

/** Colinha (#5, Natan). Hoje lista os favoritos em memória. */
class ColinhaViewModel(favorites: FavoritesRepository) : ViewModel() {
    val favorites: StateFlow<List<FavoriteCandidate>> = favorites.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ColinhaViewModel(container.favoritesRepository) }
        }
    }
}

@Composable
fun ColinhaScreen(viewModel: ColinhaViewModel, onCandidateClick: (FavoriteCandidate) -> Unit) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    if (favorites.isEmpty()) {
        EmptyState(
            title = "Sua colinha está vazia",
            message = "Favorite candidatos no detalhe para montar a sua cola.",
            icon = Icons.Filled.Star,
        )
        return
    }

    // TODO(#5): ordem da urna, regras por cargo e UF, número em "teclas" de urna, resultado e compartilhar.
    LazyColumn(Modifier.fillMaxSize()) {
        items(favorites, key = { it.id }) { favorite ->
            ListItem(
                headlineContent = { Text(favorite.ballotName.toTitleCase()) },
                supportingContent = {
                    Text(listOfNotNull(favorite.officeLabel.toTitleCase(), favorite.partyAcronym, favorite.state).joinToString(" · "))
                },
                trailingContent = { Text("${favorite.number}", style = MaterialTheme.typography.titleLarge) },
                modifier = Modifier.clickable { onCandidateClick(favorite) },
            )
        }
    }
}
