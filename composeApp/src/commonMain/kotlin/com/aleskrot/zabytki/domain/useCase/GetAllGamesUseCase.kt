package com.aleskrot.zabytki.domain.useCase

import com.aleskrot.zabytki.domain.model.Game
import com.aleskrot.zabytki.domain.repository.GamesRepository
import kotlinx.coroutines.flow.Flow

class GetAllGamesUseCase(
    private val repository: GamesRepository
) {
    operator fun invoke(): Flow<List<Game>> =
        repository.getAllGames()
}