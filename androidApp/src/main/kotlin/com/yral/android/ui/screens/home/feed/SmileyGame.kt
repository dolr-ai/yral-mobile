package com.yral.android.ui.screens.home.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.YralColors.smileyGameCardBackground
import com.yral.shared.features.game.domain.GameIcon
import com.yral.shared.features.game.domain.GameIconNames

@Composable
internal fun GameIconsRow(
    modifier: Modifier = Modifier,
    gameIcons: List<GameIcon>,
    onIconClicked: (emoji: GameIcon) -> Unit,
) {
    var clickedIcon by remember { mutableStateOf<GameIcon?>(null) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
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
                    val resourceId = it.getResource()
                    if (resourceId > 0) {
                        GameIcon(
                            modifier =
                                Modifier.clickable {
                                    if (clickedIcon == null) {
                                        clickedIcon = it
                                        onIconClicked(it)
                                    }
                                },
                            icon = resourceId,
                        )
                    }
                }
            }
        }
        clickedIcon?.let {
            BubbleAnimation(it.getResource()) {
                clickedIcon = null
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

private fun GameIcon.getResource(): Int =
    when (imageName) {
        GameIconNames.LAUGH.name -> R.drawable.laughing
        GameIconNames.HEART.name -> R.drawable.heart
        GameIconNames.FIRE.name -> R.drawable.fire
        GameIconNames.SURPRISE.name -> R.drawable.surprise
        GameIconNames.ROCKET.name -> R.drawable.rocket
        else -> 0
    }
