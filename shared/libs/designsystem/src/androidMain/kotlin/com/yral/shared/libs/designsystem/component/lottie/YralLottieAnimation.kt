package com.yral.shared.libs.designsystem.component.lottie

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
actual fun YralLottieAnimation(
    modifier: Modifier,
    rawRes: LottieRes,
    iterations: Int,
    contentScale: ContentScale,
    textReplacements: Map<String, String>,
    onAnimationComplete: () -> Unit,
) {
    val composition by
        if (textReplacements.isNotEmpty()) {
            rememberYralLottieComposition(rawRes, textReplacements)
        } else {
            rememberYralLottieComposition(rawRes)
        }
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

@Composable
actual fun rememberYralLottieComposition(
    lottieRes: LottieRes,
    textReplacements: Map<String, String>,
): State<Any?> {
    val context = LocalContext.current
    var lottieJsonString by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lottieRes) {
        lottieJsonString = readAssetAsString(context, lottieRes.assetPath)
    }

    val updatedJson =
        remember(lottieJsonString, textReplacements) {
            var mutableJson = lottieJsonString ?: ""
            textReplacements.forEach { (original, replacement) ->
                mutableJson = mutableJson.replace("\"$original\"", "\"$replacement\"")
            }
            mutableJson
        }

    return rememberLottieComposition(
        LottieCompositionSpec.JsonString(updatedJson),
    )
}

private fun readAssetAsString(
    context: Context,
    assetPath: String,
): String =
    context.assets.open(assetPath).use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            return reader.readText()
        }
    }
