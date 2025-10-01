package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
actual fun YralLottieAnimation(
    modifier: Modifier,
    rawRes: LottieRes,
    iterations: Int,
    contentScale: ContentScale,
    onAnimationComplete: () -> Unit,
) {
    val composition by rememberYralLottieComposition(rawRes)
    YralLottieAnimation(
        composition,
        modifier,
        iterations,
        contentScale,
        onAnimationComplete,
    )
}

@Composable
actual fun rememberYralLottieComposition(lottieRes: LottieRes): State<Any?> =
    rememberLottieComposition(
        LottieCompositionSpec.Asset(lottieRes.assetPath),
    )

@Composable
actual fun YralLottieAnimation(
    composition: Any?,
    modifier: Modifier,
    iterations: Int,
    contentScale: ContentScale,
    onAnimationComplete: () -> Unit,
) {
    val composition = composition as LottieComposition?
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        contentScale = contentScale,
    )

    LaunchedEffect(composition, progress, iterations) {
        if (progress == 1f && iterations == 1) {
            onAnimationComplete()
        }
    }
}
