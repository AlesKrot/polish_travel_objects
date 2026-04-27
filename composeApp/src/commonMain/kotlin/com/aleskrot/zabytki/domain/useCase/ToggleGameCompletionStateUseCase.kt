package com.aleskrot.zabytki.domain.useCase

import com.aleskrot.zabytki.domain.repository.GamesRepository

class ToggleGameCompletionStateUseCase(
    private val repository: GamesRepository
) {
    suspend operator fun invoke(
        gameId: String
    ) {
        repository.toggleGameCompletion(gameId)
    }
}