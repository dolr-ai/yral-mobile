package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.PlaybackCoordinator

@Composable
fun PlaybackLifecycleEffect(coordinator: PlaybackCoordinator) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, coordinator) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> coordinator.onAppForeground()
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    -> coordinator.onAppBackground()
                    Lifecycle.Event.ON_DESTROY -> coordinator.release()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun rememberPlaybackCoordinatorWithLifecycle(deps: CoordinatorDeps = CoordinatorDeps()): PlaybackCoordinator {
    val coordinator = rememberPlaybackCoordinator(deps)
    PlaybackLifecycleEffect(coordinator)
    return coordinator
}
