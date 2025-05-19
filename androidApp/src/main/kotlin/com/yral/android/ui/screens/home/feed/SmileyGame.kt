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
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.YralColors.smileyGameCardBackground
import com.yral.android.ui.screens.home.feed.IconAnimationConstant.ANIMATION_DURATION
import com.yral.android.ui.screens.home.feed.IconAnimationConstant.ROTATION_DEGREE
import com.yral.android.ui.screens.home.feed.IconAnimationConstant.SCALING_FACTOR
import com.yral.shared.features.game.domain.GameIcon
import com.yral.shared.features.game.domain.GameIconNames
import kotlinx.coroutines.delay
import kotlin.math.abs

private object IconAnimationConstant {
    const val ROTATION_DEGREE = -15f
    const val SCALING_FACTOR = 1.17f
    const val ANIMATION_DURATION = 200L
}

@Composable
internal fun GameIconsRow(
    modifier: Modifier = Modifier,
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon? = null,
    coinDelta: Int = 0,
    onIconClicked: (emoji: GameIcon) -> Unit,
) {
    var animateBubbles by remember { mutableStateOf(false) }
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
            if (coinDelta != 0) {
                clickedIcon?.let {
                    GameResultView(it, coinDelta)
                }
            } else {
                GameIconStrip(
                    gameIcons = gameIcons,
                    clickedIcon = clickedIcon,
                    onIconClicked = onIconClicked,
                    setAnimateBubbles = { animateBubbles = true },
                )
            }
        }
        if (animateBubbles) {
            clickedIcon?.let {
                BubbleAnimation(it.getResource()) {
                    animateBubbles = false
                }
            }
        }
    }
}

@Composable
private fun GameResultView(
    icon: GameIcon,
    coinDelta: Int,
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
        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp,
                Alignment.Start,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(
            modifier = Modifier,
            icon = icon.getResource(),
            animate = false,
            setAnimate = { },
        )
        Text(
            text = gameResultText(icon.imageName, coinDelta),
        )
    }
}

@Composable
private fun GameIconStrip(
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon? = null,
    onIconClicked: (emoji: GameIcon) -> Unit,
    setAnimateBubbles: (Boolean) -> Unit,
) {
    var animateIcon by remember { mutableStateOf(false) }
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
                            animateIcon = true
                            setAnimateBubbles(true)
                            onIconClicked(it)
                        },
                    icon = resourceId,
                    animate =
                        if (clickedIcon?.id == it.id) animateIcon else false,
                    setAnimate = { shouldAnimate -> animateIcon = shouldAnimate },
                )
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

@Composable
private fun gameResultText(
    iconName: String,
    coinDelta: Int,
): AnnotatedString =
    buildAnnotatedString {
        val textStyle = LocalAppTopography.current.mdBold
        val spanStyle =
            SpanStyle(
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily,
                fontWeight = textStyle.fontWeight,
                color = YralColors.Green50,
            )
        if (coinDelta > 0) {
            withStyle(spanStyle) {
                append(
                    stringResource(
                        R.string.was_most_people_choice,
                        iconName.lowercase().capitalize(Locale.current),
                    ),
                )
                append("\n")
            }
            withStyle(spanStyle.plus(SpanStyle(color = YralColors.Green300))) {
                append(
                    stringResource(
                        R.string.you_win_x_coins,
                        coinDelta,
                    ),
                )
            }
        } else {
            withStyle(spanStyle) {
                append(
                    stringResource(R.string.not_most_popular_pick),
                )
                append("\n")
            }
            withStyle(spanStyle.plus(SpanStyle(color = YralColors.Red300))) {
                append(
                    stringResource(
                        R.string.you_lost_x_coins,
                        abs(coinDelta),
                    ),
                )
            }
        }
    }
