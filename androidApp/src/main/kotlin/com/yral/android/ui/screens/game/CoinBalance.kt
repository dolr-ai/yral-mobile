package com.yral.android.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.game.CoinBagConstants.ANIMATION_DURATION
import com.yral.android.ui.screens.game.CoinBagConstants.COIN_BAG_ROTATION
import com.yral.android.ui.screens.game.CoinBagConstants.COIN_BAG_SCALE
import com.yral.android.ui.screens.game.CoinBagConstants.COIN_OFFSET_X_END
import com.yral.android.ui.screens.game.CoinBagConstants.COIN_OFFSET_X_MID
import com.yral.android.ui.screens.game.CoinBagConstants.COIN_OFFSET_X_START
import com.yral.android.ui.screens.game.CoinBagConstants.COIN_OFFSET_Y_END
import com.yral.android.ui.screens.game.CoinBagConstants.COIN_OFFSET_Y_MID
import com.yral.android.ui.screens.game.CoinBagConstants.COIN_OFFSET_Y_START
import com.yral.android.ui.screens.game.CoinBalanceConstants.BALANCE_TEXT_ANIMATION_DURATION
import com.yral.android.ui.screens.game.CoinBalanceConstants.BALANCE_TEXT_HEIGHT
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

private object CoinBagConstants {
    const val STATIC_BAG_SIZE = 36
    const val BAG_OFFSET_X = -22
    const val COIN_OFFSET_X_START = -80f
    const val COIN_OFFSET_X_MID = -40f
    const val COIN_OFFSET_X_END = -10f
    const val COIN_OFFSET_Y_START = 0f
    const val COIN_OFFSET_Y_MID = -40f
    const val COIN_OFFSET_Y_END = -26f
    const val ANIMATION_DURATION = 700
    const val COIN_BAG_SCALE = 1.2f
    const val COIN_BAG_ROTATION = -15f
}

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
            didWin = coinDelta > 0,
            animateBag = animateBag,
            setAnimate = setAnimate,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun CoinBag(
    didWin: Boolean,
    animateBag: Boolean,
    setAnimate: (Boolean) -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val coinYOffset = remember { Animatable(COIN_OFFSET_Y_START) }
    val coinXOffset = remember { Animatable(COIN_OFFSET_X_START) }
    val coinAlpha = remember { Animatable(0f) }

    val xStart = if (didWin) COIN_OFFSET_X_START else COIN_OFFSET_X_END
    val xEnd = if (didWin) COIN_OFFSET_X_END else COIN_OFFSET_X_START
    val yStart = if (didWin) COIN_OFFSET_Y_START else COIN_OFFSET_Y_END
    val yEnd = if (didWin) COIN_OFFSET_Y_END else COIN_OFFSET_Y_START

    LaunchedEffect(animateBag, didWin) {
        val animationSpec =
            tween<Float>(
                durationMillis = ANIMATION_DURATION / 2,
                easing = FastOutSlowInEasing,
            )
        if (animateBag) {
            scale.snapTo(1f)
            rotation.snapTo(0f)
            coinYOffset.snapTo(yStart)
            coinXOffset.snapTo(xStart)
            coinAlpha.snapTo(0f)
            awaitAll(
                async { scale.animateTo(COIN_BAG_SCALE, animationSpec = animationSpec) },
                async { rotation.animateTo(COIN_BAG_ROTATION, animationSpec = animationSpec) },
                async { coinAlpha.animateTo(1f, animationSpec = animationSpec) },
                async { coinYOffset.animateTo(COIN_OFFSET_Y_MID, animationSpec = animationSpec) },
                async { coinXOffset.animateTo(COIN_OFFSET_X_MID, animationSpec = animationSpec) },
            )
            // Animate to end
            awaitAll(
                async { scale.animateTo(1f, animationSpec = animationSpec) },
                async { rotation.animateTo(0f, animationSpec = animationSpec) },
                async { coinAlpha.animateTo(0f, animationSpec = animationSpec) },
                async { coinYOffset.animateTo(yEnd, animationSpec = animationSpec) },
                async { coinXOffset.animateTo(xEnd, animationSpec = animationSpec) },
            )
            setAnimate(false)
        }
    }

    Box {
        // Animated coins
        if (animateBag) {
            Image(
                painter = painterResource(R.drawable.gold_coins),
                contentDescription = "Animated coins",
                modifier =
                    Modifier
                        .size(24.dp)
                        .align(Alignment.BottomStart)
                        .offset {
                            IntOffset(
                                x = coinXOffset.value.dp.roundToPx(),
                                y = coinYOffset.value.dp.roundToPx(),
                            )
                        }.graphicsLayer {
                            alpha = coinAlpha.value
                        },
            )
        }
        // Animated bag
        Image(
            painter = painterResource(R.drawable.coin_bag),
            contentDescription = "Static coin bag",
            modifier =
                Modifier
                    .size(CoinBagConstants.STATIC_BAG_SIZE.dp)
                    .offset(x = CoinBagConstants.BAG_OFFSET_X.dp)
                    .graphicsLayer {
                        alpha = 1f
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                    },
        )
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
    val newTextAnimatable = remember { Animatable(0f) }
    LaunchedEffect(animateBag) {
        if (animateBag) {
            oldBalance = coinBalance - coinDelta
            newBalance = coinBalance
            isAnimating = true
            newTextAnimatable.snapTo(0f)
            newTextAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = BALANCE_TEXT_ANIMATION_DURATION),
            )
            isAnimating = false
        } else {
            isAnimating = false
        }
    }
    Box {
        if (isAnimating) {
            val progress = (1f - newTextAnimatable.value)
            val translationY = BALANCE_TEXT_HEIGHT * (progress - 1f)
            Text(
                text = oldBalance.toString(),
                style = LocalAppTopography.current.feedCanisterId,
                color = YralColors.PrimaryContainer,
                modifier =
                    Modifier
                        .graphicsLayer {
                            this.alpha = progress
                            this.translationY = translationY
                        },
            )
            val newProgress = newTextAnimatable.value
            val newTranslationY = BALANCE_TEXT_HEIGHT * (1f - newProgress)
            Text(
                text = newBalance.toString(),
                style = LocalAppTopography.current.feedCanisterId,
                color = getAnimatingTextColor(isAnimating, coinDelta),
                modifier =
                    Modifier
                        .graphicsLayer {
                            this.alpha = newProgress
                            this.translationY = newTranslationY
                        },
            )
        } else {
            Text(
                text = newBalance.toString(),
                style = LocalAppTopography.current.feedCanisterId,
                color = YralColors.PrimaryContainer,
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
