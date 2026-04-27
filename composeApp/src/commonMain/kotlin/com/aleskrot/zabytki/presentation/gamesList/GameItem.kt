package com.aleskrot.zabytki.presentation.gamesList

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aleskrot.zabytki.domain.model.Game
import com.aleskrot.zabytki.presentation.theme.Bronze
import com.aleskrot.zabytki.presentation.theme.Dimens
import org.jetbrains.compose.resources.painterResource
import zabytki.composeapp.generated.resources.Res
import zabytki.composeapp.generated.resources.franko
import zabytki.composeapp.generated.resources.medal_of_honor
import zabytki.composeapp.generated.resources.star_craft

private val gameBoxes = mapOf(
    "streets_of_rage_4" to Res.drawable.franko,
    "medal_of_honor" to Res.drawable.medal_of_honor
)
@Composable
fun GameItem(
    game: Game,
    onGameUrlClick: (Game) -> Unit,
    onRunGameButtonClick: (Game) -> Unit,
    onToggleCompletionButtonClick: (Game) -> Unit
) {
    val gameBoxRes = gameBoxes.getOrElse(game.id) {
        Res.drawable.medal_of_honor }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(Dimens.GameBoxWidth)
    ) {
        Image(
            painter = painterResource(gameBoxRes),
            contentDescription = game.title,
            modifier = Modifier
                .height(Dimens.GameBoxHeight)
                .width(Dimens.GameBoxWidth)
        )
        Box(
            modifier = Modifier
                .background(Bronze)
                .width(Dimens.GameBoxWidth)
                .padding(vertical = Dimens.PaddingSmall),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(Dimens.GameBoxWidth)
            ) {
                Image(
                    painter =
                        painterResource(Res.drawable.star_craft),
                    contentDescription = "GOG",
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .clickable { onGameUrlClick(game) }
                )
                Image(
                    painter =
                        painterResource(Res.drawable.franko),
                    contentDescription = "Play",
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .clickable { onRunGameButtonClick(game) }
                )
                val completionButton =
                    if (game.completed) Res.drawable.star_craft
                    else Res.drawable.franko
                Image(
                    painter = painterResource(completionButton),
                    contentDescription = "Completion",
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .clickable {
                            onToggleCompletionButtonClick(game) }
                )
            }
        }
    }
}
