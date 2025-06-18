package com.yral.android.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.LottieCompositionFactory
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
    val context = LocalContext.current
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(rawRes),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
    )

    DisposableEffect(Unit) {
        onDispose {
            LottieCompositionFactory.clearCache(context)
        }
    }

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        contentScale = contentScale,
    )

    LaunchedEffect(progress) {
        if (progress == 1f && iterations == 1) {
            onAnimationComplete()
        }
    }
}
