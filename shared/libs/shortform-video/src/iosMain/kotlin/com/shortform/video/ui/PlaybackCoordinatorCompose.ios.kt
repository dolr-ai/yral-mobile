package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.shortform.video.CoordinatorDeps
import com.shortform.video.PlaybackCoordinator
import com.shortform.video.ios.createIosPlaybackCoordinator

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
