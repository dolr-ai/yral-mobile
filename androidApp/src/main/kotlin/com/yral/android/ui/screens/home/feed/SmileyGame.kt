package com.yral.android.ui.screens.home.feed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.YralColors.smileyGameCardBackground
import com.yral.android.ui.screens.home.feed.IconAnimationConstant.ANIMATION_DURATION
import com.yral.android.ui.screens.home.feed.IconAnimationConstant.ROTATION_DEGREE
import com.yral.android.ui.screens.home.feed.IconAnimationConstant.SCALING_FACTOR
import com.yral.shared.features.game.domain.GameIcon
import com.yral.shared.features.game.domain.GameIconNames
import kotlinx.coroutines.delay

private object IconAnimationConstant {
    const val ROTATION_DEGREE = -15f
    const val SCALING_FACTOR = 1.17f
    const val ANIMATION_DURATION = 200L
}

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
                    var animate by remember { mutableStateOf(false) }
                    if (resourceId > 0) {
                        GameIcon(
                            modifier =
                                Modifier.clickable {
                                    if (clickedIcon == null) {
                                        animate = true
                                        clickedIcon = it
                                        onIconClicked(it)
                                    }
                                },
                            icon = resourceId,
                            animate = animate,
                            setAnimate = { shouldAnimate -> animate = shouldAnimate },
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
    animate: Boolean = false,
    setAnimate: (Boolean) -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (animate) ROTATION_DEGREE else 0f,
        animationSpec = tween(durationMillis = ANIMATION_DURATION.toInt()),
        label = "rotation",
    )
    val scale by animateFloatAsState(
        targetValue = if (animate) SCALING_FACTOR else 1f,
        animationSpec = tween(durationMillis = ANIMATION_DURATION.toInt()),
        label = "scale",
    )
    LaunchedEffect(animate) {
        delay(ANIMATION_DURATION)
        setAnimate(false)
    }
    Image(
        modifier =
            modifier
                .size(46.dp)
                .graphicsLayer(
                    rotationZ = rotation,
                    scaleX = scale,
                    scaleY = scale,
                ),
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
