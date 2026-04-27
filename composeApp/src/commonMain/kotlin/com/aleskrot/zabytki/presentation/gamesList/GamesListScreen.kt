package com.aleskrot.zabytki.presentation.gamesList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aleskrot.zabytki.presentation.theme.Dimens

@Composable
fun GamesListScreen(
    modifier: Modifier,
    viewModel: GamesListViewModel
) {
    val gamesState = viewModel.games.collectAsState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize =
            Dimens.GameBoxWidth),
        modifier = modifier,
        contentPadding = PaddingValues(Dimens.PaddingMedium),
        horizontalArrangement =
            Arrangement.spacedBy(Dimens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(gamesState.value) { game ->
            GameItem(
                game = game,
                onGameUrlClick = {
                    viewModel.handleGameUrlClick(it.gogUrl)
                },
                onRunGameButtonClick = {
                    viewModel.handleRunGameButtonClick(it.id)
                },
                onToggleCompletionButtonClick = {
//                    viewModel.handleToggleGameCompletionButtonClick(it.id)
                }
            )
        }
    }
}
