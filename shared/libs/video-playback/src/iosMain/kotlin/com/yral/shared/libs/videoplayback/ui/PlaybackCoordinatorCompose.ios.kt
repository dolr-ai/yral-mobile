package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.PlaybackCoordinator
import com.yral.shared.libs.videoplayback.ios.createIosPlaybackCoordinator

@Composable
actual fun rememberPlaybackCoordinator(
    deps: CoordinatorDeps,
): PlaybackCoordinator {
    val coordinator = remember(deps) {
        createIosPlaybackCoordinator(deps)
    }
    DisposableEffect(coordinator) {
        onDispose { coordinator.release() }
    }
    return coordinator
}
