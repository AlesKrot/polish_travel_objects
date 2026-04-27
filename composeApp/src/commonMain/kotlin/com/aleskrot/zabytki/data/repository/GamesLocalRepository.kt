package com.aleskrot.zabytki.data.repository

import com.aleskrot.zabytki.data.dataSource.GamesLocalDataSource
import com.aleskrot.zabytki.domain.model.Game
import com.aleskrot.zabytki.domain.repository.GamesRepository
import kotlinx.coroutines.flow.Flow

class GamesLocalRepository : GamesRepository {
    override fun getAllGames(): Flow<List<Game>> =
        GamesLocalDataSource.games
    override suspend fun toggleGameCompletion(
        gameId: String
    ) {
        GamesLocalDataSource.toggleGameCompletion(gameId)
    }
}