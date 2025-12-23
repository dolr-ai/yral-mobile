package com.yral.shared.libs.leaderboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.leaderboard.ui.main.LeaderboardUiConstants

@Composable
fun LeaderboardConfetti(
    showConfetti: Boolean,
    confettiAnimationComplete: () -> Unit,
) {
    if (showConfetti) {
        var count by remember { mutableIntStateOf(0) }
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            val size = LocalWindowInfo.current.containerSize.width / LeaderboardUiConstants.CONFETTI_SIZE_FACTOR
            val density = LocalDensity.current
            repeat(LeaderboardUiConstants.NO_OF_CONFETTI) { index ->
                key(count) {
                    YralLottieAnimation(
                        rawRes = LottieRes.COLORFUL_CONFETTI_BRUST,
                        contentScale = ContentScale.Crop,
                        iterations = 1,
                        onAnimationComplete = {
                            if (index == 0) {
                                if (count < LeaderboardUiConstants.CONFETTI_ITERATIONS) {
                                    count++
                                } else {
                                    confettiAnimationComplete()
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .size(with(density) { size.toDp() })
                                .scale(LeaderboardUiConstants.CONFETTI_SCALE)
                                .align(if (index % 2 == 0) Alignment.Start else Alignment.End),
                    )
                }
            }
        }
    }
}
