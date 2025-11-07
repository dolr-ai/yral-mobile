package com.yral.shared.libs.videoPlayer.pool

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.koin.compose.koinInject

/**
 * Multiplatform composable to create a PlayerPool instance
 * Uses platform-specific implementations under the hood
 */
@Composable
fun rememberPlayerPool(
    maxPoolSize: Int = 3,
    platformPlayerFactory: PlatformPlayerFactory = koinInject(),
    platformMediaSourceFactory: PlatformMediaSourceFactory = koinInject(),
): PlayerPool {
    val pool = remember(maxPoolSize, platformPlayerFactory, platformMediaSourceFactory) {
        PlayerPool(platformPlayerFactory, platformMediaSourceFactory, maxPoolSize)
    }
    DisposableEffect(pool) {
        onDispose {
            pool.dispose() // or pool.clear() depending on your API
        }
    }
    return pool
}
