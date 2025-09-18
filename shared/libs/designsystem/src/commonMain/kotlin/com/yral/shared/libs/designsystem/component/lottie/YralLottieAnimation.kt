package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
expect fun YralLottieAnimation(
    modifier: Modifier = Modifier,
    rawRes: LottieRes,
    iterations: Int = Int.MAX_VALUE,
    contentScale: ContentScale = ContentScale.FillBounds,
    onAnimationComplete: () -> Unit = {},
)

@Composable
expect fun YralLottieAnimation(
    composition: Any?,
    modifier: Modifier = Modifier,
    iterations: Int = Int.MAX_VALUE,
    contentScale: ContentScale = ContentScale.FillBounds,
    onAnimationComplete: () -> Unit = {},
)

@Composable
expect fun rememberYralLottieComposition(lottieRes: LottieRes): State<Any?>
