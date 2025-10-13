package com.yral.shared.features.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.analytics.events.GameType
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.features.game.generated.resources.Res
import yral_mobile.shared.features.game.generated.resources.ic_game_hot
import yral_mobile.shared.features.game.generated.resources.ic_game_smiley

@Composable
fun GameToggle(
    gameType: GameType,
    modifier: Modifier = Modifier,
    onSelectGame: (gameType: GameType) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.5.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .background(color = YralColors.GameToggleBackground, shape = RoundedCornerShape(size = 24.75.dp))
                .padding(start = 4.5.dp, top = 4.5.dp, end = 4.5.dp, bottom = 4.5.dp),
    ) {
        GameIcon(
            drawable = Res.drawable.ic_game_hot,
            isSelected = gameType == GameType.HOT_OR_NOT,
            onSelectGame = { onSelectGame(GameType.HOT_OR_NOT) },
        )
        GameIcon(
            drawable = Res.drawable.ic_game_smiley,
            isSelected = gameType == GameType.SMILEY,
            onSelectGame = { onSelectGame(GameType.SMILEY) },
        )
    }
}

@Composable
private fun GameIcon(
    drawable: DrawableResource,
    isSelected: Boolean,
    onSelectGame: () -> Unit,
) {
    val background =
        if (isSelected) {
            Modifier
                .background(
                    color = YralColors.Neutral800,
                    shape = RoundedCornerShape(size = 20.25.dp),
                )
        } else {
            Modifier
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(2.45.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier =
            Modifier
                .width(GameToggleConstants.ICON_WIDTH)
                .height(GameToggleConstants.ICON_HEIGHT)
                .then(background)
                .padding(start = 9.dp, top = 6.75.dp, end = 9.dp, bottom = 6.75.dp)
                .clickable { onSelectGame() },
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = "smiley game",
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .padding(0.25.dp)
                    .size(GameToggleConstants.IMAGE_SIZE),
        )
    }
}

object GameToggleConstants {
    val IMAGE_SIZE = 28.dp
    val ICON_WIDTH = 46.dp
    val ICON_HEIGHT = 42.dp
}
