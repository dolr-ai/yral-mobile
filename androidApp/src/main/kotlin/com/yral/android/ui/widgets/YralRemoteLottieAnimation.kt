package com.yral.android.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject

@Suppress("LongMethod")
@Composable
fun YralRemoteLottieAnimation(
    modifier: Modifier = Modifier,
    url: String,
    iterations: Int = LottieConstants.IterateForever,
    contentScale: ContentScale = ContentScale.FillBounds,
    enableCache: Boolean = true,
    onAnimationComplete: () -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onLoading: () -> Unit = {},
    placeholder: @Composable (() -> Unit)? = null,
    errorContent: @Composable ((Throwable) -> Unit)? = null,
    crashlyticsManager: CrashlyticsManager = koinInject(),
) {
    val context = LocalContext.current
    val logger = yralLottieLogger()
    var loadingState by remember(url) { mutableStateOf<LottieLoadingState>(LottieLoadingState.Loading) }

    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(url),
        onRetry = { _, exception ->
            handleLottieError(exception, url, crashlyticsManager, logger, onError) { error ->
                loadingState = LottieLoadingState.Error(error)
            }
            false // Let our timeout handle retries
        },
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = composition != null && loadingState is LottieLoadingState.Success,
    )

    LaunchedEffect(url) {
        logger.d { "Loading Lottie animation: $url" }
        loadingState = LottieLoadingState.Loading
        onLoading()
        withTimeoutOrNull(YralRemoteLottieAnimationConstants.TIME_OUT_MILLIS) {
            while (composition == null) {
                delay(YralRemoteLottieAnimationConstants.TIMEOUT_CHECK_DELAY)
            }
        } ?: run {
            if (loadingState is LottieLoadingState.Loading) {
                val timeoutError = YralException("Timeout loading Lottie animation: $url")
                handleLottieError(timeoutError, url, crashlyticsManager, logger, onError) { error ->
                    loadingState = LottieLoadingState.Error(error)
                }
            }
        }
    }

    // Update state when composition loads
    LaunchedEffect(composition) {
        if (composition != null) {
            loadingState = LottieLoadingState.Success
        }
    }

    // Animation completion
    LaunchedEffect(progress, iterations) {
        if (progress == 1f && iterations != LottieConstants.IterateForever) {
            logger.d { "Lottie animation completed: $url" }
            onAnimationComplete()
        }
    }

    // Cache cleanup
    DisposableEffect(url, enableCache) {
        onDispose {
            if (!enableCache) {
                clearLottieCache(context, logger)
            }
        }
    }

    // Render UI
    val currentState = loadingState
    when (currentState) {
        is LottieLoadingState.Loading -> {
            placeholder?.invoke()
        }

        is LottieLoadingState.Success -> {
            composition?.let {
                LottieAnimation(
                    composition = it,
                    progress = { progress },
                    modifier = modifier,
                    contentScale = contentScale,
                )
            }
        }

        is LottieLoadingState.Error -> {
            errorContent?.invoke(currentState.throwable)
        }
    }
}

private inline fun handleLottieError(
    exception: Throwable?,
    url: String,
    crashlyticsManager: CrashlyticsManager,
    logger: Logger,
    onError: (Throwable) -> Unit,
    onStateUpdate: (Throwable) -> Unit,
) {
    val error = exception ?: YralException("Unknown error loading Lottie animation: $url")
    logger.e(error) { "Failed to load Lottie animation: $url" }
    crashlyticsManager.recordException(error as Exception)
    onError(error)
    onStateUpdate(error)
}

private fun clearLottieCache(
    context: Context,
    logger: Logger,
) = runCatching {
    LottieCompositionFactory.clearCache(context, false)
    logger.d { "Lottie cache cleared successfully" }
}.onFailure { e -> logger.e(e) { "Failed to clear Lottie cache" } }

private sealed class LottieLoadingState {
    data object Loading : LottieLoadingState()
    data object Success : LottieLoadingState()
    data class Error(
        val throwable: Throwable,
    ) : LottieLoadingState()
}

private object YralRemoteLottieAnimationConstants {
    const val TIME_OUT_MILLIS: Long = 30_000L
    const val TIMEOUT_CHECK_DELAY: Long = 100L
}
