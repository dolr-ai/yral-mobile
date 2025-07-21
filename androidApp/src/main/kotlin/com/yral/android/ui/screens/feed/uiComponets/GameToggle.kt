package com.yral.android.ui.screens.feed.uiComponets

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.feed.uiComponets.GameToggleConstants.ICON_HEIGHT
import com.yral.android.ui.screens.feed.uiComponets.GameToggleConstants.ICON_WIDTH
import com.yral.android.ui.screens.feed.uiComponets.GameToggleConstants.IMAGE_SIZE
import com.yral.shared.analytics.events.GameType

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
                .width(105.75.dp)
                .height(50.625.dp)
                .background(color = YralColors.GameToggleBackground, shape = RoundedCornerShape(size = 24.75.dp))
                .padding(start = 4.5.dp, top = 4.5.dp, end = 4.5.dp, bottom = 4.5.dp),
    ) {
        GameIcon(
            drawable = R.drawable.ic_game_hot,
            isSelected = gameType == GameType.HOT_OR_NOT,
            onSelectGame = { onSelectGame(GameType.HOT_OR_NOT) },
        )
        GameIcon(
            drawable = R.drawable.ic_game_smiley,
            isSelected = gameType == GameType.SMILEY,
            onSelectGame = { onSelectGame(GameType.SMILEY) },
        )
    }
}

@Composable
private fun GameIcon(
    drawable: Int,
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
                .width(ICON_WIDTH)
                .height(ICON_HEIGHT)
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
                    .size(IMAGE_SIZE),
        )
    }
}

object GameToggleConstants {
    val IMAGE_SIZE = 28.125.dp
    val ICON_WIDTH = 46.125.dp
    val ICON_HEIGHT = 41.625.dp
}
