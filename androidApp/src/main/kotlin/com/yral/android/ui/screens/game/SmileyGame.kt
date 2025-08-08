package com.yral.android.ui.screens.game

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.game.SmileyGameConstants.NUDGE_ANIMATION_DURATION
import com.yral.android.ui.screens.game.SmileyGameConstants.NUDGE_ANIMATION_ICON_ITERATIONS
import com.yral.android.ui.widgets.YralFeedback
import com.yral.shared.features.game.domain.models.GameIcon
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun SmileyGame(
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon?,
    isLoading: Boolean,
    coinDelta: Int = 0,
    errorMessage: String = "",
    onIconClicked: (emoji: GameIcon, isTutorialVote: Boolean) -> Unit,
    hasShownCoinDeltaAnimation: Boolean,
    onDeltaAnimationComplete: () -> Unit,
    shouldShowNudge: Boolean,
    pageNo: Int,
    onNudgeAnimationComplete: () -> Unit,
) {
    var animateBubbles by remember { mutableStateOf(false) }
    var iconPositions by remember { mutableStateOf(mapOf<Int, Float>()) }
    val bubbleAnimationComplete =
        !animateBubbles || (clickedIcon?.getBubbleResource() == 0 && clickedIcon.clickAnimation.isEmpty())
    val resultViewVisible = (coinDelta != 0 || errorMessage.isNotEmpty()) && bubbleAnimationComplete
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        when {
            resultViewVisible -> {
                SmileyGameResult(
                    clickedIcon = clickedIcon,
                    coinDelta = coinDelta,
                    errorMessage = errorMessage,
                    originalPos = iconPositions[gameIcons.indexOf(clickedIcon)] ?: 0f,
                    hasShownCoinDeltaAnimation = hasShownCoinDeltaAnimation,
                    onAnimationComplete = onDeltaAnimationComplete,
                )
            }
            else -> {
                var animatingNudgeIconPosition by remember { mutableStateOf<Int?>(null) }
                var nudgeIterationCount by remember { mutableStateOf(0) }
                SmileyGameNudge(
                    pageNo = pageNo,
                    shouldShowNudge = shouldShowNudge,
                    animatingNudgeIconPosition = animatingNudgeIconPosition,
                    startNudgeAnimation = {
                        animatingNudgeIconPosition = 0
                        nudgeIterationCount = 0
                    },
                    dismissNudgeAnimation = {
                        animatingNudgeIconPosition = null
                        nudgeIterationCount = 0
                    },
                )
                GameIconStrip(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    gameIcons = gameIcons,
                    clickedIcon = clickedIcon,
                    onIconClicked = {
                        animateBubbles = true
                        onIconClicked(it, animatingNudgeIconPosition != null)
                    },
                    isLoading = isLoading,
                    coinDelta = coinDelta,
                    onIconPositioned = { id, xPos ->
                        iconPositions = iconPositions.plus(id to xPos)
                    },
                    animatingNudgeIconPosition = animatingNudgeIconPosition,
                    onIconAnimationComplete = {
                        animatingNudgeIconPosition =
                            animatingNudgeIconPosition?.let { currentIndex ->
                                when {
                                    currentIndex + 1 < gameIcons.size -> currentIndex + 1
                                    ++nudgeIterationCount >= NUDGE_ANIMATION_ICON_ITERATIONS ->
                                        run {
                                            nudgeIterationCount = 0
                                            onNudgeAnimationComplete()
                                            null
                                        }
                                    else -> 0
                                }
                            }
                    },
                    setNudgeShown = {
                        animatingNudgeIconPosition = null
                        nudgeIterationCount = 0
                        onNudgeAnimationComplete()
                    },
                )
            }
        }
        if (animateBubbles) {
            clickedIcon?.let {
                GameIconBubbles(clickedIcon) {
                    animateBubbles = false
                }
            }
        }
    }
    if (resultViewVisible && !hasShownCoinDeltaAnimation) {
        YralFeedback(
            sound = if (coinDelta > 0) R.raw.spilled_coin else R.raw.coin_loss,
            withHapticFeedback = true,
            hapticFeedbackType = HapticFeedbackType.LongPress,
        )
    }
}

@Composable
private fun BoxScope.SmileyGameResult(
    clickedIcon: GameIcon?,
    coinDelta: Int,
    errorMessage: String,
    originalPos: Float,
    hasShownCoinDeltaAnimation: Boolean,
    onAnimationComplete: () -> Unit,
) {
    clickedIcon?.let {
        GameResultView(
            modifier = Modifier.align(Alignment.BottomCenter),
            icon = it,
            coinDelta = coinDelta,
            errorMessage = errorMessage,
            originalPos = originalPos,
        )
    }
    if (!hasShownCoinDeltaAnimation && errorMessage.isEmpty()) {
        CoinDeltaAnimation(
            text = coinDelta.toSignedString(),
            textColor =
                if (coinDelta > 0) {
                    YralColors.Green300.copy(alpha = 0.3f)
                } else {
                    YralColors.Red300.copy(alpha = 0.3f)
                },
            onAnimationEnd = onAnimationComplete,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BoxScope.SmileyGameNudge(
    pageNo: Int,
    shouldShowNudge: Boolean,
    animatingNudgeIconPosition: Int?,
    startNudgeAnimation: () -> Unit,
    dismissNudgeAnimation: () -> Unit,
) {
    LaunchedEffect(shouldShowNudge) {
        try {
            if (shouldShowNudge) {
                startNudgeAnimation()
            } else {
                dismissNudgeAnimation()
            }
        } catch (e: CancellationException) {
            // Reset state safely on coroutine cancellation
            dismissNudgeAnimation()
            throw e
        } catch (_: Exception) {
            dismissNudgeAnimation()
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "nudge $pageNo")
    val tweenSpec =
        tween<Float>(
            durationMillis = NUDGE_ANIMATION_DURATION.toInt(),
            easing = FastOutLinearInEasing,
        )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
    )
    animatingNudgeIconPosition?.let {
        SmileyGameNudgeContent(
            modifier = Modifier.align(Alignment.BottomCenter),
            alpha = alpha,
            offsetY = offsetY,
        )
    }
}

@Composable
private fun SmileyGameNudgeContent(
    modifier: Modifier,
    alpha: Float,
    offsetY: Float,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(YralColors.ScrimColorLight)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val density = LocalDensity.current
        var textWidth by remember { mutableIntStateOf(0) }
        Image(
            painter = painterResource(id = R.drawable.smiley_game_nudge_stars),
            contentDescription = null,
            modifier =
                Modifier
                    .padding(bottom = 226.dp)
                    .width(with(density) { textWidth.toDp() + 16.dp })
                    .height(130.dp)
                    .alpha(alpha),
            contentScale = ContentScale.FillBounds,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp, start = 36.dp, end = 36.dp)
                    .offset(y = offsetY.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    buildAnnotatedString {
                        val textStyle = LocalAppTopography.current.xlBold
                        val spanStyle =
                            SpanStyle(
                                fontSize = textStyle.fontSize,
                                fontFamily = textStyle.fontFamily,
                                fontWeight = textStyle.fontWeight,
                                color = YralColors.Neutral50,
                            )
                        withStyle(spanStyle) { append(stringResource(R.string.smiley_game_nudge_1)) }
                        withStyle(spanStyle) { append("\n") }
                        withStyle(style = spanStyle.copy(color = YralColors.Yellow200)) {
                            append(stringResource(R.string.smiley_game_nudge_2))
                        }
                    },
                textAlign = TextAlign.Center,
                modifier = Modifier.onGloballyPositioned { textWidth = it.size.width },
            )
            Image(
                painter = painterResource(id = R.drawable.smiley_game_nudge_arrow),
                contentDescription = "arrow",
                modifier = Modifier,
            )
        }
    }
}

private fun Int.toSignedString(): String =
    if (this >= 0) {
        "+$this"
    } else {
        "$this"
    }

object SmileyGameConstants {
    const val NUDGE_ANIMATION_DURATION = 600L
    const val NUDGE_ANIMATION_ICON_ITERATIONS = 3
}
