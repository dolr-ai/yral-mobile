package com.yral.shared.libs.videoPlayer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import co.touchlab.kermit.Logger

fun getPlayerLifecycleObserver(
    player: PlatformPlayer,
    isPause: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit,
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME ->
                handleOnResume(
                    player = player,
                    isPause = isPause,
                    wasAppInBackground = wasAppInBackground,
                    setWasAppInBackground = setWasAppInBackground,
                )

            Lifecycle.Event.ON_PAUSE -> handleOnPause(player, setWasAppInBackground)
            Lifecycle.Event.ON_STOP -> handleOnStop(player, setWasAppInBackground)
            Lifecycle.Event.ON_DESTROY -> handleOnDestroy(player)
            else -> { }
        }
    }

private fun handleOnResume(
    player: PlatformPlayer,
    isPause: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit,
) {
    Logger.d("PlayerLifecycle") { "WasAppInBackground $wasAppInBackground" }
    player.prepare()
    if (isPause) {
        player.pause()
    } else {
        player.play()
    }
    setWasAppInBackground(false)
}

private fun handleOnPause(
    player: PlatformPlayer,
    setWasAppInBackground: (Boolean) -> Unit,
) {
    player.pause()
    player.stop()
    setWasAppInBackground(true)
}

private fun handleOnStop(
    player: PlatformPlayer,
    setWasAppInBackground: (Boolean) -> Unit,
) {
    player.pause()
    player.stop()
    setWasAppInBackground(true)
}

private fun handleOnDestroy(player: PlatformPlayer) {
    player.release()
}
