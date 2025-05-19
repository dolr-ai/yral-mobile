package com.yral.android.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import com.yral.android.ui.screens.game.IconAnimationConstant.ANIMATION_DURATION
import com.yral.android.ui.screens.game.IconAnimationConstant.RESULT_ANIMATION_DURATION
import com.yral.android.ui.screens.game.IconAnimationConstant.ROTATION_DEGREE
import com.yral.android.ui.screens.game.IconAnimationConstant.SCALING_FACTOR
import com.yral.shared.features.game.domain.GameIcon
import com.yral.shared.features.game.domain.GameIconNames
import kotlinx.coroutines.delay
import kotlin.math.abs

private object IconAnimationConstant {
    const val ROTATION_DEGREE = -15f
    const val SCALING_FACTOR = 1.17f
    const val ANIMATION_DURATION = 200L
    const val RESULT_ANIMATION_DURATION = 400L
}

@Composable
fun GameIconsRow(
    modifier: Modifier = Modifier,
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon? = null,
    coinDelta: Int = 0,
    onIconClicked: (emoji: GameIcon) -> Unit,
) {
    var animateBubbles by remember { mutableStateOf(false) }
    var iconPositions by remember { mutableStateOf(mapOf<Int, Float>()) }

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
                    GameResultView(
                        icon = clickedIcon,
                        coinDelta = coinDelta,
                        originalPos =
                            iconPositions[gameIcons.indexOfFirst { clickedIcon.id == it.id }] ?: 0f,
                    )
                }
            } else {
                GameIconStrip(
                    gameIcons = gameIcons,
                    clickedIcon = clickedIcon,
                    onIconClicked = onIconClicked,
                    setAnimateBubbles = { animate -> animateBubbles = animate },
                    onIconPositioned = { id, xPos ->
                        // Store position of each icon for later animation
                        iconPositions = iconPositions + (id to xPos)
                    },
                )
            }
        }
        GameIconBubbles(clickedIcon, animateBubbles) { animate ->
            animateBubbles = animate
        }
        GameResultAnimation(coinDelta = coinDelta)
    }
}

@Composable
private fun GameIconBubbles(
    clickedIcon: GameIcon?,
    animateBubbles: Boolean,
    setAnimateBubbles: (Boolean) -> Unit,
) {
    if (animateBubbles) {
        clickedIcon?.let {
            BubbleAnimation(it.getResource()) {
                setAnimateBubbles(false)
            }
        }
    }
}

@Composable
private fun GameResultAnimation(coinDelta: Int) {
    var animateCoinDelta by remember { mutableStateOf(false) }
    LaunchedEffect(coinDelta) {
        if (coinDelta != 0) {
            animateCoinDelta = true
        }
    }
    if (animateCoinDelta) {
        CoinDeltaAnimation(
            text = coinDelta.toSignedString(),
            textColor =
                if (coinDelta > 0) {
                    YralColors.Green300
                } else {
                    YralColors.Red300
                },
        ) {
            animateCoinDelta = false
        }
    }
}

private fun Int.toSignedString(): String =
    if (this >= 0) {
        "+$this"
    } else {
        "$this"
    }

@Composable
private fun GameResultView(
    icon: GameIcon,
    coinDelta: Int,
    originalPos: Float,
) {
    var animate by remember { mutableStateOf(true) }
    val iconOffsetX = remember { Animatable(originalPos) }
    LaunchedEffect(coinDelta) {
        if (coinDelta != 0) {
            iconOffsetX.snapTo(originalPos)
            iconOffsetX.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(
                        durationMillis = RESULT_ANIMATION_DURATION.toInt(),
                        easing = FastOutLinearInEasing,
                    ),
            )
            delay(RESULT_ANIMATION_DURATION)
            animate = false
        } else {
            animate = false
            iconOffsetX.snapTo(0f)
        }
    }
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
            modifier =
                Modifier
                    .graphicsLayer {
                        translationX = iconOffsetX.value
                    },
            icon = icon.getResource(),
            animate = false,
            setAnimate = { },
        )
        if (iconOffsetX.value == 0f) {
            Text(
                text = gameResultText(icon.imageName, coinDelta),
            )
        }
    }
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

@Composable
private fun GameIconStrip(
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon? = null,
    onIconClicked: (emoji: GameIcon) -> Unit,
    setAnimateBubbles: (Boolean) -> Unit,
    onIconPositioned: (Int, Float) -> Unit = { _, _ -> },
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
        gameIcons.forEachIndexed { index, icon ->
            val resourceId = icon.getResource()
            if (resourceId > 0) {
                Box(
                    modifier =
                        Modifier
                            .onGloballyPositioned { coordinates ->
                                onIconPositioned(index, coordinates.positionInParent().x)
                            },
                ) {
                    GameIcon(
                        modifier =
                            Modifier.clickable {
                                animateIcon = true
                                setAnimateBubbles(true)
                                onIconClicked(icon)
                            },
                        icon = resourceId,
                        animate =
                            if (clickedIcon?.id == icon.id) animateIcon else false,
                        setAnimate = { shouldAnimate -> animateIcon = shouldAnimate },
                    )
                }
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
