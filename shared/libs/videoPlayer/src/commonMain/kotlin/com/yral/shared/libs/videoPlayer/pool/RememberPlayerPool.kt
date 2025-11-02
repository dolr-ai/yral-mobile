package com.yral.shared.libs.videoPlayer.pool

import androidx.compose.runtime.Composable
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
): PlayerPool =
    remember(maxPoolSize) {
        PlayerPool(platformPlayerFactory, platformMediaSourceFactory, maxPoolSize)
    }
