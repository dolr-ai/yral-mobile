package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.koin.koinInstance
import io.github.alexzhirkevich.compottie.CompottieException
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.Url
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import org.koin.compose.koinInject

@Composable
fun PreloadLottieAnimation(
    url: String,
    crashlyticsManager: CrashlyticsManager = koinInject(),
) {
    val logger = yralLottieLogger()
    val result =
        rememberLottieComposition(url) {
            LottieCompositionSpec.Url(url)
        }
    LaunchedEffect(result) {
        try {
            result.await()
            // logger.d { "Successfully preloaded: $url" }
        } catch (e: CompottieException) {
            logger.e(e) { "Preload failed with exception: $url" }
            val error = YralException("Failed to preload Lottie animation: $url", e)
            crashlyticsManager.recordException(error)
        }
    }
}

@Composable
fun PreloadLottieAnimations(urls: List<String>) {
    urls.forEach { url -> PreloadLottieAnimation(url) }
}

internal fun yralLottieLogger(): Logger = koinInstance.get<YralLogger>().withTag("YralLottie")
