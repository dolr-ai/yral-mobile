package com.yral.android.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun YralLottieAnimation(
    modifier: Modifier = Modifier,
    rawRes: Int,
    iterations: Int = LottieConstants.IterateForever,
    contentScale: ContentScale = ContentScale.FillBounds,
    onAnimationComplete: () -> Unit = {},
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(rawRes),
    )
    YralLottieAnimation(
        composition,
        modifier,
        iterations,
        contentScale,
        onAnimationComplete,
    )
}

@Composable
fun YralLottieAnimation(
    composition: LottieComposition?,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    contentScale: ContentScale = ContentScale.FillBounds,
    onAnimationComplete: () -> Unit = {},
) {
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
