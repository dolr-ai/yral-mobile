package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import yral_mobile.shared.libs.designsystem.generated.resources.Res

@Composable
fun YralLottieAnimation(
    modifier: Modifier = Modifier,
    rawRes: LottieRes,
    iterations: Int = Int.MAX_VALUE,
    contentScale: ContentScale = ContentScale.FillBounds,
    onAnimationComplete: () -> Unit = {},
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
fun rememberYralLottieComposition(lottieRes: LottieRes): State<Any?> =
    rememberLottieComposition {
        if (lottieRes.path.endsWith(".lottie")) {
            LottieCompositionSpec.DotLottie(
                Res.readBytes(lottieRes.path),
            )
        } else {
            LottieCompositionSpec.JsonString(
                Res.readBytes(lottieRes.path).decodeToString(),
            )
        }
    }

@Composable
fun YralLottieAnimation(
    composition: Any?,
    modifier: Modifier = Modifier,
    iterations: Int = Int.MAX_VALUE,
    contentScale: ContentScale = ContentScale.FillBounds,
    onAnimationComplete: () -> Unit = {},
) {
    val composition = composition as LottieComposition?
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
    )
    Image(
        painter =
            rememberLottiePainter(
                composition = composition,
                progress = { progress },
            ),
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = "Lottie animation",
    )

    LaunchedEffect(composition, progress, iterations) {
        if (progress == 1f && iterations == 1) {
            onAnimationComplete()
        }
    }
}
