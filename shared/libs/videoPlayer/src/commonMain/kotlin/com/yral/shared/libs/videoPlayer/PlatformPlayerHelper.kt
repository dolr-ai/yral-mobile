package com.yral.shared.libs.videoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer
import com.yral.shared.libs.videoPlayer.pool.PlayerPool
import com.yral.shared.libs.videoPlayer.pool.VideoListener

@Suppress("LongMethod")
@Composable
fun rememberPooledPlatformPlayer(
    playerData: PlayerData,
    playerPool: PlayerPool,
    isPause: Boolean,
    videoListener: VideoListener?,
): PlatformPlayer? {
    var platformPlayer: PlatformPlayer? by remember { mutableStateOf(null) }

    // Get player from pool when URL changes
    LaunchedEffect(playerData.url, playerPool) {
        if (playerData.url.isNotEmpty()) {
            platformPlayer =
                playerPool
                    .getPlayer(
                        playerData = playerData,
                        videoListener = videoListener,
                    )
        }
    }

    // Note: Performance tracing is now handled internally by the PlayerPool
    // No need for additional tracing here to avoid duplicates

    val lifecycleOwner = LocalLifecycleOwner.current
    var appInBackground by remember {
        mutableStateOf(false)
    }
    platformPlayer?.let { currentPlatformPlayer ->
        DisposableEffect(lifecycleOwner, appInBackground, currentPlatformPlayer) {
            val lifecycleObserver =
                getPlayerLifecycleObserver(currentPlatformPlayer, isPause, appInBackground) {
                    appInBackground = it
                }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            }
        }
    }

    return platformPlayer
}
