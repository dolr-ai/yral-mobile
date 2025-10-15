package com.yral.shared.libs.designsystem.component.lottie

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.Url
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun YralRemoteLottieAnimation(
    modifier: Modifier = Modifier.Companion,
    url: String,
    iterations: Int = Int.MAX_VALUE,
    contentScale: ContentScale = ContentScale.Companion.FillBounds,
    onAnimationComplete: () -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onLoading: () -> Unit = {},
    placeholder: @Composable (() -> Unit)? = null,
    errorContent: @Composable ((Throwable) -> Unit)? = null,
    crashlyticsManager: CrashlyticsManager = koinInject(),
) {
    val logger = yralLottieLogger()

    val compositionResult =
        rememberLottieComposition {
            LottieCompositionSpec.Url(url)
        }

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
        if (progress == 1f && iterations != Int.MAX_VALUE) {
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
            Image(
                painter =
                    rememberLottiePainter(
                        composition = compositionResult.value,
                        progress = { progress },
                    ),
                modifier = modifier,
                contentScale = contentScale,
                contentDescription = "Lottie animation",
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
