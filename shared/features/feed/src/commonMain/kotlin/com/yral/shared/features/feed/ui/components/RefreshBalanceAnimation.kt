package com.yral.shared.features.feed.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.component.lottie.rememberYralLottieComposition
import com.yral.shared.libs.designsystem.theme.YralColors

@Composable
fun RefreshBalanceAnimation(
    refreshBalanceState: RefreshBalanceAnimationState,
    onAnimationComplete: () -> Unit,
) {
    AnimatedVisibility(
        visible = refreshBalanceState != RefreshBalanceAnimationState.HIDDEN,
        modifier = Modifier.Companion.fillMaxSize(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // Preload result animations to prevent flicker
        val successComposition by rememberYralLottieComposition(
            LottieRes.CLAIM_SUCCESSFUL_WO_LOADING,
        )
        val failureComposition by rememberYralLottieComposition(
            LottieRes.CLAIM_UNSUCCESSFUL_WO_LOADING,
        )

        Box(
            modifier =
                Modifier.Companion
                    .fillMaxSize()
                    .background(YralColors.ScrimColor)
                    .clickable {},
        ) {
            when (refreshBalanceState) {
                RefreshBalanceAnimationState.LOADING -> {
                    YralLottieAnimation(
                        rawRes = LottieRes.COMMON_LOADING,
                        modifier = Modifier.Companion.fillMaxSize(),
                    )
                }

                RefreshBalanceAnimationState.SUCCESS -> {
                    YralLottieAnimation(
                        composition = successComposition,
                        iterations = 1,
                        modifier = Modifier.Companion.fillMaxSize(),
                        onAnimationComplete = onAnimationComplete,
                    )
                }

                RefreshBalanceAnimationState.FAILURE -> {
                    YralLottieAnimation(
                        composition = failureComposition,
                        iterations = 1,
                        modifier = Modifier.Companion.fillMaxSize(),
                        onAnimationComplete = onAnimationComplete,
                    )
                }

                RefreshBalanceAnimationState.HIDDEN -> {
                    // This case is handled by the early return
                }
            }
        }
    }
}

enum class RefreshBalanceAnimationState {
    HIDDEN,
    LOADING,
    SUCCESS,
    FAILURE,
}
