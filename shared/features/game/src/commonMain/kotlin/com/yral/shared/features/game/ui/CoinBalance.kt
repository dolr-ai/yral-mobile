package com.yral.shared.features.game.ui

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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yral.shared.features.game.ui.CoinBagConstants.ANIMATION_DURATION
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_BAG_ROTATION
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_BAG_SCALE
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_OFFSET_X_END
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_OFFSET_X_MID
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_OFFSET_X_START
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_OFFSET_Y_END
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_OFFSET_Y_MID
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_OFFSET_Y_START
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_SCALE
import com.yral.shared.features.game.ui.CoinBagConstants.COIN_SCALE_MID
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.features.game.generated.resources.Res
import yral_mobile.shared.features.game.generated.resources.coin_bag
import yral_mobile.shared.features.game.generated.resources.gold_coins

private object CoinBagConstants {
    const val STATIC_BAG_SIZE = 36
    const val BAG_OFFSET_X = -18
    const val COIN_OFFSET_X_START = -50f
    const val COIN_OFFSET_X_MID = -25f
    const val COIN_OFFSET_X_END = -10f
    const val COIN_OFFSET_Y_START = 0f
    const val COIN_OFFSET_Y_MID = -28f
    const val COIN_OFFSET_Y_END = -26f
    const val ANIMATION_DURATION = 700
    const val COIN_BAG_SCALE = 1.2f
    const val COIN_SCALE = 1.8f
    const val COIN_SCALE_MID = 1.4f
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
        modifier = modifier,
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
    val coinScale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val coinYOffset = remember { Animatable(COIN_OFFSET_Y_START) }
    val coinXOffset = remember { Animatable(COIN_OFFSET_X_START) }
    val coinAlpha = remember { Animatable(0f) }

    val xStart = if (didWin) COIN_OFFSET_X_START else COIN_OFFSET_X_END
    val xEnd = if (didWin) COIN_OFFSET_X_END else COIN_OFFSET_X_START
    val yStart = if (didWin) COIN_OFFSET_Y_START else COIN_OFFSET_Y_END
    val yEnd = if (didWin) COIN_OFFSET_Y_END else COIN_OFFSET_Y_START
    val coinStart = if (didWin) COIN_SCALE else 1f
    val coinEnd = if (didWin) 1f else COIN_SCALE

    var coinVisible by remember { mutableStateOf(false) }

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
            coinScale.snapTo(coinStart)
            coinVisible = true
            awaitAll(
                async { scale.animateTo(COIN_BAG_SCALE, animationSpec = animationSpec) },
                async { rotation.animateTo(COIN_BAG_ROTATION, animationSpec = animationSpec) },
                async { coinAlpha.animateTo(1f, animationSpec = animationSpec) },
                async { coinYOffset.animateTo(COIN_OFFSET_Y_MID, animationSpec = animationSpec) },
                async { coinXOffset.animateTo(COIN_OFFSET_X_MID, animationSpec = animationSpec) },
                async { coinScale.animateTo(COIN_SCALE_MID, animationSpec = animationSpec) },
            )
            // Animate to end
            awaitAll(
                async { scale.animateTo(1f, animationSpec = animationSpec) },
                async { rotation.animateTo(0f, animationSpec = animationSpec) },
                async { coinAlpha.animateTo(0f, animationSpec = animationSpec) },
                async { coinYOffset.animateTo(yEnd, animationSpec = animationSpec) },
                async { coinXOffset.animateTo(xEnd, animationSpec = animationSpec) },
                async { coinScale.animateTo(coinEnd, animationSpec = animationSpec) },
            )
            setAnimate(false)
            coinVisible = false
        }
    }

    Box {
        // Animated bag
        Image(
            painter = painterResource(Res.drawable.coin_bag),
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
        // Animated coins
        if (coinVisible) {
            Image(
                painter = painterResource(Res.drawable.gold_coins),
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
                        }.scale(coinScale.value)
                        .graphicsLayer {
                            // alpha = coinAlpha.value
                        },
            )
        }
    }
}

@Composable
private fun BoxScope.Balance(coinBalance: Long) {
    Column(
        modifier =
            Modifier
                .height(32.dp)
                .widthIn(min = 75.dp)
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
            text = formatAbbreviation(balance),
            style = LocalAppTopography.current.feedCanisterId,
            color = animatedColor,
            overflow = TextOverflow.Ellipsis,
        )
    }
    LaunchedEffect(coinBalance) {
        delay(ANIMATION_DURATION.toLong())
        previousBalance = coinBalance
    }
}
