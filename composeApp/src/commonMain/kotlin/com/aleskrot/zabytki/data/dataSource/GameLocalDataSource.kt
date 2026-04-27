package com.aleskrot.zabytki.data.dataSource

import com.aleskrot.zabytki.domain.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object GamesLocalDataSource {
    private var _games = MutableStateFlow(
        listOf(
            Game(
                id = "star_craft",
                title = "StarCraft",
                gogUrl = "https://www.gog.com/pl/game/streets_of_rage_4"
//            platforms = listOf(Platform.PC),
//            imageRes = Res.drawable.star_craft
            ),
            Game(
                id = "medal_of_honor",
                title = "Medal of Honor",
                gogUrl =
                    "https://www.gog.com/pl/game/medal_of_honor_allied_assault_war_c hest",
//                platforms = listOf(Platform.PC, Platform.ANBERNIC),
//                imageRes = Res.drawable.franko
                completed = true
            ),
            Game(
                id = "medal_of_honor",
                title = "Medal of Honor",
                gogUrl = "https://www.gog.com/pl/game/medal_of_honor_allied_assault_war_chest",
//                platforms = listOf(Platform.PC, Platform.ANBERNIC),
//                imageRes = Res.drawable.medal_of_honor
        )
        )
    )
    val games: StateFlow<List<Game>> = _games.asStateFlow()
    fun toggleGameCompletion(
        gameId: String
    ) {
        _games.update { gamesList ->
            gamesList.map { game ->
                if (game.id == gameId) {
                    game.copy(completed = !game.completed)
                } else {
                    game
                }
            }
        }
    }
}