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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.game.SmileyGameConstants.NUDGE_ANIMATION_DURATION
import com.yral.android.ui.screens.game.SmileyGameConstants.NUDGE_DURATION
import com.yral.android.ui.widgets.YralFeedback
import com.yral.shared.features.game.domain.models.GameIcon
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongMethod")
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
    val resultViewVisible = (coinDelta != 0 || errorMessage.isNotEmpty()) // && !animateBubbles
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
                SmileyGameNudge(
                    pageNo = pageNo,
                    shouldShowNudge = shouldShowNudge,
                    gameIcons = gameIcons,
                    animatingNudgeIconPosition = animatingNudgeIconPosition,
                    setAnimatingNudgeIconPosition = { animatingNudgeIconPosition = it },
                    onNudgeAnimationComplete = onNudgeAnimationComplete,
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
                    onStripAnimationComplete = {
                        animatingNudgeIconPosition =
                            animatingNudgeIconPosition
                                ?.let { if (it < gameIcons.size) it + 1 else 0 }
                    },
                    setNudgeShown = {
                        animatingNudgeIconPosition = null
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
    gameIcons: List<GameIcon>,
    animatingNudgeIconPosition: Int?,
    setAnimatingNudgeIconPosition: (Int?) -> Unit,
    onNudgeAnimationComplete: () -> Unit,
) {
    LaunchedEffect(shouldShowNudge) {
        try {
            if (shouldShowNudge) {
                for (index in gameIcons.indices) {
                    setAnimatingNudgeIconPosition(index)
                    delay(NUDGE_DURATION)
                }
                setAnimatingNudgeIconPosition(null)
                onNudgeAnimationComplete()
            } else {
                setAnimatingNudgeIconPosition(null)
            }
        } catch (e: CancellationException) {
            // Reset state safely on coroutine cancellation
            setAnimatingNudgeIconPosition(null)
            throw e
        } catch (_: Exception) {
            setAnimatingNudgeIconPosition(null)
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
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            painter = painterResource(id = R.drawable.smiley_game_nudge_stars),
            contentDescription = null,
            modifier =
                Modifier
                    .padding(bottom = 256.dp)
                    .fillMaxWidth()
                    .alpha(alpha),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(id = R.drawable.smiley_game_nudge),
            contentDescription = null,
            modifier =
                Modifier
                    .padding(bottom = 100.dp)
                    .fillMaxWidth()
                    .offset(y = offsetY.dp),
            contentScale = ContentScale.Fit,
        )
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
    const val NUDGE_DURATION = 5000L
}
