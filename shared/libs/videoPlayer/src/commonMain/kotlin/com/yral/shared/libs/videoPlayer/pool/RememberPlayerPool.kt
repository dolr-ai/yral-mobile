package com.yral.shared.libs.videoPlayer.pool

import androidx.compose.runtime.Composable

/**
 * Multiplatform composable to create a PlayerPool instance
 * Uses platform-specific implementations under the hood
 */
@Composable
expect fun rememberPlayerPool(maxPoolSize: Int = 3): PlayerPool
