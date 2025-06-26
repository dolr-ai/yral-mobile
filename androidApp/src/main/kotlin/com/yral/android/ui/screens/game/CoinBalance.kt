package com.yral.android.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.game.CoinBalanceConstants.BALANCE_TEXT_ANIMATION_DURATION
import com.yral.android.ui.screens.game.CoinBalanceConstants.BALANCE_TEXT_HEIGHT
import com.yral.android.ui.widgets.YralLottieAnimation
import kotlinx.coroutines.launch

@Composable
fun CoinBalance(
    coinBalance: Long,
    coinDelta: Int,
    animateBag: Boolean,
    setAnimate: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(top = 22.dp, bottom = 22.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Balance(
            coinBalance = coinBalance,
            coinDelta = coinDelta,
            animateBag = animateBag,
        )
        CoinBag(
            coinDelta = coinDelta,
            animateBag = animateBag,
            setAnimate = setAnimate,
        )
    }
}

@Composable
private fun CoinBag(
    coinDelta: Int,
    animateBag: Boolean,
    setAnimate: (Boolean) -> Unit,
) {
    if (!animateBag) {
        Image(
            painter = painterResource(R.drawable.coin_bag),
            contentDescription = "coin bag",
            modifier =
                Modifier
                    .size(36.dp)
                    .offset(x = (-22).dp),
        )
    } else {
        val bagAnimationRes =
            if (coinDelta > 0) R.raw.smiley_game_win else R.raw.smiley_game_lose
        var animate by remember { mutableStateOf(true) }
        AnimatedVisibility(
            visible = animate,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            YralLottieAnimation(
                modifier =
                    Modifier
                        .height(52.dp)
                        .width(105.dp)
                        .offset(
                            x = (-16).dp,
                            y = (-12).dp,
                        ).graphicsLayer {
                            clip = false
                            scaleX = 2f
                            scaleY = 2f
                        },
                rawRes = bagAnimationRes,
                iterations = 1,
            ) {
                setAnimate(false)
                animate = false
            }
        }
    }
}

@Composable
private fun BoxScope.Balance(
    coinBalance: Long,
    coinDelta: Int,
    animateBag: Boolean,
) {
    Column(
        modifier =
            Modifier
                .height(32.dp)
                .background(
                    brush =
                        Brush.linearGradient(
                            listOf(
                                YralColors.CoinBalanceBGStart,
                                YralColors.CoinBalanceBGEnd,
                            ),
                        ),
                    shape = RoundedCornerShape(16.dp),
                ).padding(start = 22.dp, end = 10.dp)
                .align(Alignment.CenterEnd),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        BalanceText(
            coinBalance = coinBalance,
            coinDelta = coinDelta,
            animateBag = animateBag,
        )
    }
}

@Composable
private fun BalanceText(
    coinBalance: Long,
    coinDelta: Int,
    animateBag: Boolean,
) {
    var oldBalance by remember { mutableLongStateOf(coinBalance) }
    var newBalance by remember { mutableLongStateOf(coinBalance) }
    var isAnimating by remember { mutableStateOf(false) }
    var hasAnimated by remember { mutableStateOf(false) }
    val newTextAnimatable = remember { Animatable(0f) }
    LaunchedEffect(animateBag) {
        if (animateBag) {
            oldBalance = coinBalance - coinDelta
            newBalance = coinBalance
            isAnimating = true
            newTextAnimatable.snapTo(0f)
            launch {
                newTextAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = BALANCE_TEXT_ANIMATION_DURATION),
                )
                hasAnimated = true
            }
        } else {
            isAnimating = false
        }
    }
    Box {
        if (!hasAnimated && (!isAnimating || (1f - newTextAnimatable.value) > 0f)) {
            val progress = (1f - newTextAnimatable.value)
            val translationY = BALANCE_TEXT_HEIGHT * (progress - 1f)
            Text(
                text = oldBalance.toString(),
                style = LocalAppTopography.current.feedCanisterId,
                color = YralColors.PrimaryContainer,
                modifier =
                    Modifier
                        .graphicsLayer {
                            this.alpha = if (isAnimating) progress else 1f
                            this.translationY = if (isAnimating) translationY else 0f
                        },
            )
        }
        if (isAnimating || hasAnimated) {
            val progress = newTextAnimatable.value
            val translationY = BALANCE_TEXT_HEIGHT * (1f - progress)
            Text(
                text = newBalance.toString(),
                style = LocalAppTopography.current.feedCanisterId,
                color = getAnimatingTextColor(isAnimating, coinDelta),
                modifier =
                    Modifier
                        .graphicsLayer {
                            this.alpha = if (isAnimating) progress else 1f
                            this.translationY = if (isAnimating) translationY else 0f
                        },
            )
        }
    }
}

private fun getAnimatingTextColor(
    isAnimating: Boolean,
    coinDelta: Int,
): Color =
    when {
        isAnimating -> if (coinDelta < 0) YralColors.Red300 else YralColors.Green400
        else -> YralColors.PrimaryContainer
    }

private object CoinBalanceConstants {
    const val BALANCE_TEXT_HEIGHT = 30f
    const val BALANCE_TEXT_ANIMATION_DURATION = 500
}
