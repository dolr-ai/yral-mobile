package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import co.touchlab.kermit.Logger
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
actual fun YralRemoteLottieAnimation(
    modifier: Modifier,
    url: String,
    iterations: Int,
    contentScale: ContentScale,
    onAnimationComplete: () -> Unit,
    onError: (Throwable) -> Unit,
    onLoading: () -> Unit,
    placeholder: @Composable (() -> Unit)?,
    errorContent: @Composable ((Throwable) -> Unit)?,
    crashlyticsManager: CrashlyticsManager,
) {
    val logger = yralLottieLogger()

    val compositionResult =
        rememberLottieComposition(
            spec = LottieCompositionSpec.Url(url),
            onRetry = { _, exception ->
                handleLottieError(exception, url, crashlyticsManager, logger, onError)
                false // Let our timeout handle retries
            },
        )

    val progress by animateLottieCompositionAsState(
        composition = compositionResult.value,
        iterations = iterations,
        isPlaying = compositionResult.value != null && !compositionResult.isLoading && compositionResult.error == null,
    )

    LaunchedEffect(compositionResult.isLoading) {
        if (compositionResult.isLoading) {
            logger.d { "Loading Lottie animation: $url" }
            onLoading()
            withTimeoutOrNull(YralRemoteLottieAnimationConstants.TIME_OUT_MILLIS) {
                while (compositionResult.isLoading && compositionResult.error == null) {
                    delay(YralRemoteLottieAnimationConstants.TIMEOUT_CHECK_DELAY)
                }
            } ?: run {
                if (compositionResult.isLoading) {
                    val timeoutError = YralException("Timeout loading Lottie animation: $url")
                    handleLottieError(timeoutError, url, crashlyticsManager, logger, onError)
                }
            }
        }
    }

    LaunchedEffect(compositionResult.error) {
        compositionResult.error?.let { error ->
            handleLottieError(error, url, crashlyticsManager, logger, onError)
        }
    }

    // Animation completion
    LaunchedEffect(progress, iterations) {
        if (progress == 1f && iterations != LottieConstants.IterateForever) {
            logger.d { "Lottie animation completed: $url" }
            onAnimationComplete()
        }
    }

    // Render UI based on LottieCompositionResult state
    when {
        compositionResult.isLoading -> {
            placeholder?.invoke()
        }

        compositionResult.error != null -> {
            errorContent?.invoke(compositionResult.error!!)
        }

        compositionResult.value != null -> {
            LottieAnimation(
                composition = compositionResult.value!!,
                progress = { progress },
                modifier = modifier,
                contentScale = contentScale,
            )
        }
    }
}

private inline fun handleLottieError(
    exception: Throwable?,
    url: String,
    crashlyticsManager: CrashlyticsManager,
    logger: Logger,
    onError: (Throwable) -> Unit,
) {
    val error = exception ?: YralException("Unknown error loading Lottie animation: $url")
    logger.e(error) { "Failed to load Lottie animation: $url" }
    crashlyticsManager.recordException(error as Exception)
    onError(error)
}

private object YralRemoteLottieAnimationConstants {
    const val TIME_OUT_MILLIS: Long = 30_000L
    const val TIMEOUT_CHECK_DELAY: Long = 100L
}
