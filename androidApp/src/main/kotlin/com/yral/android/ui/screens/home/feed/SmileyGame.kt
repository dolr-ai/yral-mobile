package com.yral.android.ui.screens.home.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.YralColors.smileyGameCardBackground

@Composable
internal fun GameIconsRow(
    modifier: Modifier = Modifier,
    gameIcons: List<GameIcon> = iconList(),
    onIconClicked: (emoji: GameIcon) -> Unit,
) {
    Row(
        modifier =
            modifier
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp,
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = smileyGameCardBackground,
                        shape = RoundedCornerShape(size = 49.dp),
                    ).padding(
                        horizontal = 12.dp,
                        vertical = 4.dp,
                    ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            gameIcons.forEach {
                GameIcon(
                    modifier = Modifier.clickable { onIconClicked(it) },
                    icon = it.drawable,
                )
            }
        }
    }
}

@Composable
private fun GameIcon(
    modifier: Modifier,
    icon: Int,
) {
    Image(
        modifier =
            modifier
                .size(46.dp),
        painter = painterResource(id = icon),
        contentDescription = "image description",
        contentScale = ContentScale.FillBounds,
    )
}

data class GameIcon(
    val drawable: Int,
    val url: String,
    val position: Offset,
)

private fun iconList() =
    listOf(
        GameIcon(
            drawable = R.drawable.laughing,
            url = "",
            position = Offset.Zero,
        ),
        GameIcon(
            drawable = R.drawable.heart,
            url = "",
            position = Offset.Zero,
        ),
        GameIcon(
            drawable = R.drawable.fire,
            url = "",
            position = Offset.Zero,
        ),
        GameIcon(
            drawable = R.drawable.surprise,
            url = "",
            position = Offset.Zero,
        ),
        GameIcon(
            drawable = R.drawable.rocket,
            url = "",
            position = Offset.Zero,
        ),
    )
