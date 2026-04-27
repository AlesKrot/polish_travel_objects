package com.aleskrot.zabytki.domain.repository

import com.aleskrot.zabytki.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GamesRepository {
    fun getAllGames(): Flow<List<Game>>
    suspend fun toggleGameCompletion(gameId: String)
}