package com.yral.android.ui.screens.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

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
private fun BoxScope.Balance(coinBalance: Long) {
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
        )
    }
}

@Composable
private fun BalanceText(coinBalance: Long) {
    var previousBalance by remember { mutableLongStateOf(coinBalance) }
    val delta = coinBalance - previousBalance
    // Animate color based on delta using animateColorAsState
    val targetColor =
        when {
            delta > 0 -> YralColors.Green400
            delta < 0 -> YralColors.Red300
            else -> YralColors.PrimaryContainer
        }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = ANIMATION_DURATION),
        label = "BalanceColor",
    )
    AnimatedContent(
        targetState = coinBalance,
        transitionSpec = {
            (slideInVertically { it } + fadeIn(tween(ANIMATION_DURATION))) togetherWith
                (slideOutVertically { -it } + fadeOut(tween(ANIMATION_DURATION))) using
                SizeTransform(clip = false)
        },
        label = "CoinBalanceChange",
    ) { balance ->
        Text(
            text = balance.toString(),
            style = LocalAppTopography.current.feedCanisterId,
            color = animatedColor,
        )
    }
    LaunchedEffect(coinBalance) {
        delay(ANIMATION_DURATION.toLong())
        previousBalance = coinBalance
    }
}
