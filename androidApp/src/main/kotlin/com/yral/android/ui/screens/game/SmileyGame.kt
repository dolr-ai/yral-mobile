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
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.YralColors.SmileyGameCardBackground
import com.yral.android.ui.screens.game.IconAnimationConstant.ANIMATION_DURATION
import com.yral.android.ui.screens.game.IconAnimationConstant.RESULT_ANIMATION_DURATION
import com.yral.android.ui.screens.game.IconAnimationConstant.ROTATION_DEGREE
import com.yral.android.ui.screens.game.IconAnimationConstant.SCALING_FACTOR
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.android.ui.widgets.YralRemoteLottieAnimation
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
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
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon?,
    isLoading: Boolean,
    coinDelta: Int = 0,
    errorMessage: String = "",
    onIconClicked: (emoji: GameIcon) -> Unit,
) {
    var animateBubbles by remember { mutableStateOf(false) }
    var iconPositions by remember { mutableStateOf(mapOf<Int, Float>()) }
    var animateCoinDelta by remember { mutableStateOf(false) }
    var resultViewVisible by remember(coinDelta, errorMessage) {
        mutableStateOf(shouldShowResult(coinDelta, errorMessage))
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (!resultViewVisible || animateBubbles) {
            GameIconStrip(
                modifier = Modifier.align(Alignment.BottomCenter),
                gameIcons = gameIcons,
                clickedIcon = clickedIcon,
                onIconClicked = onIconClicked,
                isLoading = isLoading,
                coinDelta = coinDelta,
                setAnimateBubbles = { animate -> animateBubbles = animate },
                onIconPositioned = { id, xPos ->
                    // Store position of each icon for later animation
                    iconPositions = iconPositions + (id to xPos)
                },
            )
        } else {
            clickedIcon?.let {
                GameResultView(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    icon = clickedIcon,
                    coinDelta = coinDelta,
                    errorMessage = errorMessage,
                    originalPos =
                        iconPositions[gameIcons.indexOfFirst { clickedIcon.id == it.id }] ?: 0f,
                )
            }
        }
        GameIconBubbles(clickedIcon, animateBubbles) { animate ->
            animateBubbles = animate
        }
        if (!animateBubbles) {
            GameResultAnimation(
                coinDelta,
                animateCoinDelta = animateCoinDelta,
                resultViewVisible = resultViewVisible,
                setAnimateCoinDelta = { animateCoinDelta = it },
                onAnimationComplete = {
                    resultViewVisible = shouldShowResult(coinDelta, errorMessage)
                },
            )
        }
    }
}

private fun shouldShowResult(
    coinDelta: Int,
    errorMessage: String,
) = coinDelta != 0 || errorMessage.isNotEmpty()

@Composable
private fun GameIconBubbles(
    clickedIcon: GameIcon?,
    animateBubbles: Boolean,
    setAnimateBubbles: (Boolean) -> Unit,
) {
    if (animateBubbles) {
        clickedIcon?.let {
//            BubbleAnimation(it.getResource()) {
//                setAnimateBubbles(false)
//            }
            var playLocalAnimation by remember { mutableStateOf(it.clickAnimation.isEmpty()) }
            if (!playLocalAnimation) {
                YralRemoteLottieAnimation(
                    modifier = Modifier.fillMaxSize(),
                    url = it.clickAnimation,
                    iterations = 1,
                    onAnimationComplete = {
                        setAnimateBubbles(false)
                        Logger.d("xxxx Lottie Animation completed")
                    },
                    onError = { error ->
                        playLocalAnimation = true
                        Logger.e("xxxx Lottie Error loading animation", error)
                    },
                    onLoading = {
                        Logger.d("xxxx Lottie Loading animation...")
                    },
                )
            } else {
                val animationRes = it.getBubbleResource()
                YralLottieAnimation(
                    modifier = Modifier.fillMaxSize(),
                    rawRes = animationRes,
                    iterations = 1,
                ) {
                    setAnimateBubbles(false)
                }
            }
        }
    }
}

@Composable
private fun GameResultAnimation(
    coinDelta: Int,
    animateCoinDelta: Boolean,
    resultViewVisible: Boolean,
    setAnimateCoinDelta: (Boolean) -> Unit,
    onAnimationComplete: () -> Unit,
) {
    LaunchedEffect(coinDelta) {
        if (coinDelta != 0 && !resultViewVisible) {
            setAnimateCoinDelta(true)
        }
    }
    if (animateCoinDelta) {
        CoinDeltaAnimation(
            text = coinDelta.toSignedString(),
            textColor =
                if (coinDelta > 0) {
                    YralColors.Green300.copy(alpha = 0.3f)
                } else {
                    YralColors.Red300.copy(alpha = 0.3f)
                },
        ) {
            setAnimateCoinDelta(false)
            onAnimationComplete()
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
    modifier: Modifier,
    icon: GameIcon,
    coinDelta: Int,
    errorMessage: String = "",
    originalPos: Float,
) {
    var animate by remember { mutableStateOf(true) }
    val iconOffsetX = remember { Animatable(originalPos) }
    LaunchedEffect(coinDelta) {
        if (shouldShowResult(coinDelta, errorMessage)) {
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
            animate = true
            iconOffsetX.snapTo(originalPos)
        }
    }
    GameIconStripBackground(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp,
                Alignment.Start,
            ),
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
                text = gameResultText(icon.imageName, coinDelta, errorMessage),
            )
        }
    }
}

@Composable
private fun gameResultText(
    iconName: GameIconNames,
    coinDelta: Int,
    errorMessage: String = "",
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
        if (errorMessage.isNotEmpty()) {
            withStyle(spanStyle.plus(SpanStyle(color = YralColors.Red300))) {
                append(errorMessage)
            }
            return@buildAnnotatedString
        }
        if (coinDelta > 0) {
            withStyle(spanStyle) {
                append(
                    stringResource(
                        R.string.was_most_people_choice,
                        iconName.name.lowercase().capitalize(Locale.current),
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
    modifier: Modifier,
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon? = null,
    onIconClicked: (emoji: GameIcon) -> Unit,
    isLoading: Boolean,
    coinDelta: Int = 0,
    setAnimateBubbles: (Boolean) -> Unit,
    onIconPositioned: (Int, Float) -> Unit = { _, _ -> },
) {
    var animateIcon by remember { mutableStateOf(false) }
    GameIconStripBackground(modifier) {
        gameIcons.forEachIndexed { index, icon ->
            var loadLocal by remember { mutableStateOf(false) }
            val resourceId = icon.getResource()
            LaunchedEffect(icon.imageUrl) {
                if (icon.imageUrl.isEmpty() && !loadLocal) {
                    loadLocal = true
                }
            }

            Box(
                modifier =
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            onIconPositioned(index, coordinates.positionInParent().x)
                        },
            ) {
                AsyncGameIcon(
                    modifier =
                        Modifier.clickable {
                            if (coinDelta == 0 && !isLoading) {
                                animateIcon = true
                                setAnimateBubbles(true)
                                onIconClicked(icon)
                            }
                        },
                    icon = icon.imageUrl,
                    animate =
                        if (clickedIcon?.id == icon.id) animateIcon else false,
                    setAnimate = { shouldAnimate -> animateIcon = shouldAnimate },
                    loadLocal = { loadLocal = true },
                )
                if (loadLocal && resourceId > 0) {
                    GameIcon(
                        modifier =
                            Modifier.clickable {
                                if (coinDelta == 0 && !isLoading) {
                                    animateIcon = true
                                    setAnimateBubbles(true)
                                    onIconClicked(icon)
                                }
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

@Composable
private fun AsyncGameIcon(
    modifier: Modifier,
    icon: String,
    animate: Boolean = false,
    setAnimate: (Boolean) -> Unit,
    loadLocal: () -> Unit,
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
    AsyncImage(
        model = icon,
        modifier =
            modifier
                .size(46.dp)
                .graphicsLayer(
                    rotationZ = rotation,
                    scaleX = scale,
                    scaleY = scale,
                ),
        contentDescription = "image description",
        contentScale = ContentScale.FillBounds,
        onError = { loadLocal() },
    )
}

@Composable
private fun GameIconStripBackground(
    modifier: Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable () -> Unit,
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
                        color = SmileyGameCardBackground,
                        shape = RoundedCornerShape(size = 49.dp),
                    ).padding(
                        horizontal = 12.dp,
                        vertical = 4.dp,
                    ),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

fun GameIcon.getResource(): Int =
    when (imageName) {
        GameIconNames.LAUGH -> R.drawable.laughing
        GameIconNames.HEART -> R.drawable.heart
        GameIconNames.FIRE -> R.drawable.fire
        GameIconNames.SURPRISE -> R.drawable.surprise
        GameIconNames.ROCKET -> R.drawable.rocket
        else -> 0
    }

private fun GameIcon.getBubbleResource(): Int =
    when (imageName) {
        GameIconNames.LAUGH -> R.raw.smiley_game_laugh
        GameIconNames.HEART -> R.raw.smiley_game_heart
        GameIconNames.FIRE -> R.raw.smiley_game_fire
        GameIconNames.SURPRISE -> R.raw.smiley_game_surprise
        GameIconNames.ROCKET -> R.raw.smiley_game_rocket
        else -> 0
    }
