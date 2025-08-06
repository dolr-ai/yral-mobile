package com.yral.android.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.airbnb.lottie.LottieCompositionFactory
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.koin.koinInstance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.compose.koinInject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun PreloadLottieAnimation(
    url: String,
    onSuccess: () -> Unit = {},
    onError: (Throwable) -> Unit = {},
    crashlyticsManager: CrashlyticsManager = koinInject(),
) {
    val context = LocalContext.current
    val logger = yralLottieLogger()
    LaunchedEffect(url) {
        @Suppress("TooGenericExceptionCaught")
        try {
            preloadLottieAnimation(context, url, crashlyticsManager, logger)
            onSuccess()
        } catch (e: CancellationException) {
            // Re-throw CancellationException to properly handle coroutine cancellation
            throw e
        } catch (e: Exception) {
            onError(e)
        }
    }
}

@Composable
fun PreloadLottieAnimations(
    urls: List<String>,
    onAllSuccess: () -> Unit = {},
    onAnyError: (List<Throwable>) -> Unit = {},
    onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    crashlyticsManager: CrashlyticsManager = koinInject(),
) {
    val logger = yralLottieLogger()
    val context = LocalContext.current
    var completedCount by remember(urls) { mutableIntStateOf(0) }
    var successCount by remember(urls) { mutableIntStateOf(0) }
    val errors = remember(urls) { mutableListOf<Throwable>() }

    LaunchedEffect(urls) {
        if (urls.isEmpty()) {
            onAllSuccess()
            return@LaunchedEffect
        }

        logger.d { "Starting parallel preload of ${urls.size} Lottie animations" }

        // Reset counters
        completedCount = 0
        successCount = 0
        errors.clear()

        // Launch all preloads in parallel with progress tracking
        val jobs =
            urls.mapIndexed { index, url ->
                @Suppress("TooGenericExceptionCaught")
                async {
                    try {
                        preloadLottieAnimation(context, url, crashlyticsManager, logger)
                        // Update progress on success
                        synchronized(this@LaunchedEffect) {
                            completedCount++
                            successCount++
                            onProgress(completedCount, urls.size)
                        }
                        true // Success
                    } catch (e: CancellationException) {
                        // Re-throw CancellationException to properly handle coroutine cancellation
                        throw e
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to preload Lottie animation: $url" }
                        synchronized(this@LaunchedEffect) {
                            errors.add(e)
                            completedCount++
                            onProgress(completedCount, urls.size)
                        }
                        false // Failure
                    }
                }
            }

        // Await all results
        val results = jobs.awaitAll()
        // Process final results
        val successfulCount = results.count { it }
        val totalUrls = urls.size
        logger.d { "Preload completed: $successfulCount/$totalUrls successful" }

        if (successfulCount == totalUrls) {
            logger.d { "All Lottie animations preloaded successfully" }
            onAllSuccess()
        } else {
            logger.d { "Some Lottie animations failed to preload: ${errors.size} errors" }
            onAnyError(errors.toList())
        }
    }
}

@Suppress("TooGenericExceptionCaught")
private suspend fun preloadLottieAnimation(
    context: Context,
    url: String,
    crashlyticsManager: CrashlyticsManager,
    logger: Logger,
): Unit =
    suspendCancellableCoroutine { continuation ->
        try {
            logger.d { "Starting preload for: $url" }
            val task = LottieCompositionFactory.fromUrl(context, url)
            task.addListener { result ->
                if (result != null) {
                    logger.d { "Successfully preloaded: $url" }
                    continuation.resume(Unit)
                } else {
                    val error = YralException("Failed to preload Lottie animation: $url")
                    logger.e(error) { "Preload failed - null result: $url" }
                    crashlyticsManager.recordException(error)
                    continuation.resumeWithException(error)
                }
            }
            task.addFailureListener { error ->
                logger.e(error) { "Preload failed with exception: $url" }
                crashlyticsManager.recordException(error as Exception)
                continuation.resumeWithException(error)
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception during preload setup: $url" }
            crashlyticsManager.recordException(e)
            continuation.resumeWithException(e)
        }
    }

fun yralLottieLogger(): Logger = koinInstance.get<YralLogger>().withTag("YralLottie")
