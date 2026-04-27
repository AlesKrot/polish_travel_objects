package com.aleskrot.zabytki.presentation.gamesList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aleskrot.zabytki.data.repository.GamesLocalRepository
import com.aleskrot.zabytki.domain.model.Game
import com.aleskrot.zabytki.domain.useCase.GetAllGamesUseCase
import com.aleskrot.zabytki.domain.useCase.ToggleGameCompletionStateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class GamesListViewModel : ViewModel() {
    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
    }
    private val getAllGamesUseCase = GetAllGamesUseCase(
        repository = GamesLocalRepository()
    )
    private val toggleGameCompletionStateUseCase =
        ToggleGameCompletionStateUseCase(
            repository = GamesLocalRepository()
        )
    val games: StateFlow<List<Game>> =
        getAllGamesUseCase()
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )
    fun handleGameUrlClick(
        url: String
    ) {
//        openGameUrl(url)
    }

    fun handleRunGameButtonClick(
        gameId: String
    ) {
        viewModelScope.launch {
//            runGame(gameId)
        }
    }

    fun handleToggleCompletionButtonClick(
        game: Game
    ) {
        viewModelScope.launch {
            toggleGameCompletionStateUseCase(game.id)
        }
    }
}
