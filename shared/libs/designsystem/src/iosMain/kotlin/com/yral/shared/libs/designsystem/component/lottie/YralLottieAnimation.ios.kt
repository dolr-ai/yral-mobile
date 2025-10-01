package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
actual fun YralLottieAnimation(
    modifier: Modifier,
    rawRes: LottieRes,
    iterations: Int,
    contentScale: ContentScale,
    textReplacements: Map<String, String>,
    onAnimationComplete: () -> Unit,
) {
    onAnimationComplete()
}

@Composable
actual fun YralLottieAnimation(
    composition: Any?,
    modifier: Modifier,
    iterations: Int,
    contentScale: ContentScale,
    onAnimationComplete: () -> Unit,
) {
    onAnimationComplete()
}

@Composable
actual fun rememberYralLottieComposition(lottieRes: LottieRes): State<Any?> =
    remember {
        mutableStateOf(null)
    }

@Composable
actual fun rememberYralLottieComposition(
    lottieRes: LottieRes,
    textReplacements: Map<String, String>,
): State<Any?> =
    remember {
        mutableStateOf(null)
    }
