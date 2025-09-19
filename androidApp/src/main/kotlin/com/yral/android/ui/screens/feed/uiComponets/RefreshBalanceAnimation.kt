package com.yral.android.ui.screens.feed.uiComponets

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
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yral.android.R
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.shared.features.game.viewmodel.RefreshBalanceState
import com.yral.shared.libs.designsystem.theme.YralColors

@Composable
internal fun RefreshBalanceAnimation(
    refreshBalanceState: RefreshBalanceState,
    onAnimationComplete: () -> Unit,
) {
    AnimatedVisibility(
        visible = refreshBalanceState != RefreshBalanceState.HIDDEN,
        modifier = Modifier.Companion.fillMaxSize(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // Preload result animations to prevent flicker
        val successComposition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.claim_successful_wo_loading),
        )
        val failureComposition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.claim_unsucessful_wo_loading),
        )

        Box(
            modifier =
                Modifier.Companion
                    .fillMaxSize()
                    .background(YralColors.ScrimColor)
                    .clickable {},
        ) {
            when (refreshBalanceState) {
                RefreshBalanceState.LOADING -> {
                    YralLottieAnimation(
                        rawRes = R.raw.common_loading,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.Companion.fillMaxSize(),
                    )
                }

                RefreshBalanceState.SUCCESS -> {
                    YralLottieAnimation(
                        composition = successComposition,
                        iterations = 1,
                        modifier = Modifier.Companion.fillMaxSize(),
                        onAnimationComplete = onAnimationComplete,
                    )
                }

                RefreshBalanceState.FAILURE -> {
                    YralLottieAnimation(
                        composition = failureComposition,
                        iterations = 1,
                        modifier = Modifier.Companion.fillMaxSize(),
                        onAnimationComplete = onAnimationComplete,
                    )
                }

                RefreshBalanceState.HIDDEN -> {
                    // This case is handled by the early return
                }
            }
        }
    }
}
