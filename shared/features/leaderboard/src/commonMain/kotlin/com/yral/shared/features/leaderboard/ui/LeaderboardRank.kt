package com.yral.shared.features.leaderboard.ui

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.features.leaderboard.ui.LeaderboardRankConstants.ANIMATION_DURATION
import com.yral.shared.features.leaderboard.ui.LeaderboardRankConstants.STATIC_TROPHY_SIZE
import com.yral.shared.features.leaderboard.ui.LeaderboardRankConstants.TROPHY_OFFSET_X
import com.yral.shared.features.leaderboard.ui.LeaderboardRankConstants.TROPHY_ROTATION
import com.yral.shared.features.leaderboard.ui.LeaderboardRankConstants.TROPHY_SCALE
import com.yral.shared.libs.NumberFormatter
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.formatAbbreviation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.features.leaderboard.generated.resources.Res
import yral_mobile.shared.features.leaderboard.generated.resources.rank_golden_trophy

private object LeaderboardRankConstants {
    const val STATIC_TROPHY_SIZE = 36
    const val TROPHY_OFFSET_X = 18
    const val ANIMATION_DURATION = 700
    const val TROPHY_SCALE = 1.2f
    const val TROPHY_ROTATION = 0f
}

@Composable
fun DailyRanK(
    position: Long,
    animate: Boolean,
    setAnimate: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var previousPosition by remember { mutableLongStateOf(position) }
    val delta = position - previousPosition
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        Position(
            position = position,
            delta = delta,
            onAnimationComplete = { previousPosition = position },
        )
        Trophy(
            didWin = false,
            animateBag = animate,
            setAnimate = setAnimate,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun Trophy(
    didWin: Boolean,
    animateBag: Boolean,
    setAnimate: (Boolean) -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(animateBag, didWin) {
        val animationSpec =
            tween<Float>(
                durationMillis = ANIMATION_DURATION / 2,
                easing = FastOutSlowInEasing,
            )
        if (animateBag) {
            scale.snapTo(1f)
            rotation.snapTo(0f)
            awaitAll(
                async { scale.animateTo(TROPHY_SCALE, animationSpec = animationSpec) },
                async { rotation.animateTo(TROPHY_ROTATION, animationSpec = animationSpec) },
            )
            // Animate to end
            awaitAll(
                async { scale.animateTo(1f, animationSpec = animationSpec) },
                async { rotation.animateTo(0f, animationSpec = animationSpec) },
            )
            setAnimate(false)
        }
    }

    Box {
        // Animated trophy
        Image(
            painter = painterResource(Res.drawable.rank_golden_trophy),
            contentDescription = "Trophy",
            modifier =
                Modifier
                    .size(STATIC_TROPHY_SIZE.dp)
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
private fun BoxScope.Position(
    position: Long,
    delta: Long,
    onAnimationComplete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .height(32.dp)
                .widthIn(min = 75.dp)
                .offset(x = TROPHY_OFFSET_X.dp)
                .background(
                    brush =
                        Brush.linearGradient(
                            listOf(
                                YralColors.Red500,
                                YralColors.Red300,
                            ),
                        ),
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                ).padding(start = 16.dp, end = 10.dp)
                .align(Alignment.CenterEnd),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        PositionText(
            position = position,
            delta = delta,
            onAnimationComplete = onAnimationComplete,
        )
    }
}

@Composable
private fun PositionText(
    position: Long,
    delta: Long,
    onAnimationComplete: () -> Unit,
) {
    // Animate color based on delta using animateColorAsState
    val targetColor =
        when {
            delta > 0 -> YralColors.Green400
            delta < 0 -> YralColors.Red300
            else -> YralColors.NeutralTextPrimary
        }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = ANIMATION_DURATION),
        label = "Position color",
    )
    AnimatedContent(
        targetState = position,
        transitionSpec = {
            (slideInVertically { it } + fadeIn(tween(ANIMATION_DURATION))) togetherWith
                (slideOutVertically { -it } + fadeOut(tween(ANIMATION_DURATION))) using
                SizeTransform(clip = false)
        },
        label = "Position change",
    ) { balance ->
        Text(
            text = "#".plus(NumberFormatter().formatAbbreviation(balance)),
            style = LocalAppTopography.current.feedCanisterId,
            color = animatedColor,
            overflow = TextOverflow.Ellipsis,
        )
    }
    LaunchedEffect(position) {
        delay(ANIMATION_DURATION.toLong())
        onAnimationComplete()
    }
}
