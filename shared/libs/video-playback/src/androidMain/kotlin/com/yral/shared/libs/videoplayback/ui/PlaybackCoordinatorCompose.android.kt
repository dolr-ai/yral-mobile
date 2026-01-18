package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.PlaybackCoordinator
import com.yral.shared.libs.videoplayback.android.createAndroidPlaybackCoordinator

@Composable
actual fun rememberPlaybackCoordinator(deps: CoordinatorDeps): PlaybackCoordinator {
    val context = LocalContext.current
    val coordinator =
        remember(deps, context) {
            createAndroidPlaybackCoordinator(context, deps)
        }
    DisposableEffect(coordinator) {
        onDispose { coordinator.release() }
    }
    return coordinator
}
