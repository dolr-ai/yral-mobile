package com.yral.android.ui.screens.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.yral.android.R
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralFeedback
import com.yral.shared.features.game.domain.models.GameIcon

@Composable
fun SmileyGame(
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon?,
    isLoading: Boolean,
    coinDelta: Int = 0,
    errorMessage: String = "",
    onIconClicked: (emoji: GameIcon) -> Unit,
    hasShownCoinDeltaAnimation: Boolean,
    onDeltaAnimationComplete: () -> Unit,
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
                GameIconStrip(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    gameIcons = gameIcons,
                    clickedIcon = clickedIcon,
                    onIconClicked = {
                        animateBubbles = true
                        onIconClicked(it)
                    },
                    isLoading = isLoading,
                    coinDelta = coinDelta,
                    onIconPositioned = { id, xPos ->
                        iconPositions = iconPositions.plus(id to xPos)
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

private fun Int.toSignedString(): String =
    if (this >= 0) {
        "+$this"
    } else {
        "$this"
    }
