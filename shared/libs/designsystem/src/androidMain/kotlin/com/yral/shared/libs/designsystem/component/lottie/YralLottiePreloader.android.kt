package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.airbnb.lottie.LottieCompositionFactory
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.koin.koinInstance

@Composable
actual fun PreloadLottieAnimation(
    url: String,
    crashlyticsManager: CrashlyticsManager,
) {
    val logger = yralLottieLogger()
    val context = LocalContext.current
    LaunchedEffect(url) {
        val task = LottieCompositionFactory.fromUrl(context, url)
        task.addListener { result ->
            if (result != null) {
                logger.d { "Successfully preloaded: $url" }
            } else {
                val error = YralException("Failed to preload Lottie animation: $url")
                logger.e(error) { "Preload failed - null result: $url" }
                crashlyticsManager.recordException(error)
            }
        }
        task.addFailureListener { error ->
            logger.e(error) { "Preload failed with exception: $url" }
            crashlyticsManager.recordException(error as Exception)
        }
    }
}

internal fun yralLottieLogger(): Logger = koinInstance.get<YralLogger>().withTag("YralLottie")
