package com.yral.android.ui.widgets

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
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

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
) {
    val context = LocalContext.current
    var isLoading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<Throwable?>(null) }

    // Create composition spec with or without caching
    val compositionSpec =
        remember(url, enableCache) {
            if (enableCache) {
                LottieCompositionSpec.Url(url)
            } else {
                // For non-cached loading, you might need to handle this differently
                // depending on your specific requirements
                LottieCompositionSpec.Url(url)
            }
        }

    val composition by rememberLottieComposition(
        spec = compositionSpec,
        onRetry = { _, _ ->
            error = null
            isLoading = true
            false
        },
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = composition != null && error == null,
    )

    // Handle loading state changes
    LaunchedEffect(composition, isLoading) {
        when {
            composition != null -> {
                isLoading = false
                error = null
            }

            isLoading -> {
                onLoading()
            }
        }
    }

    // Cache management
    DisposableEffect(url, enableCache) {
        onDispose {
            if (!enableCache) {
                // Clear cache for this specific animation if caching is disabled
                LottieCompositionFactory.clearCache(context, false)
            }
        }
    }

    // Handle animation completion
    LaunchedEffect(progress) {
        if (progress == 1f && iterations == 1) {
            onAnimationComplete()
        }
    }

    // Render based on state
    when {
        isLoading && placeholder != null -> {
            placeholder()
        }

        error != null -> {
            LaunchedEffect(error) {
                error?.let { onError(it) }
            }
        }

        composition != null -> {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = modifier,
                contentScale = contentScale,
            )
        }
    }
}

@Composable
fun PreloadLottieAnimation(url: String) {
    val context = LocalContext.current
    LottieCompositionFactory.fromUrl(context, url)
}
