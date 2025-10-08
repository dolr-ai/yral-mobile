package com.yral.shared.libs.videoPlayer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer
import co.touchlab.kermit.Logger

fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    isPause: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit,
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME ->
                handleOnResume(
                    exoPlayer,
                    isPause,
                    wasAppInBackground,
                    setWasAppInBackground,
                )

            Lifecycle.Event.ON_PAUSE -> handleOnPause(exoPlayer, setWasAppInBackground)
            Lifecycle.Event.ON_STOP -> handleOnStop(exoPlayer, setWasAppInBackground)
            Lifecycle.Event.ON_DESTROY -> handleOnDestroy(exoPlayer)
            else -> { }
        }
    }

private fun handleOnResume(
    exoPlayer: ExoPlayer,
    isPause: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit,
) {
    Logger.d("ExoPlayer") { "WasAppInBackground $wasAppInBackground" }
    exoPlayer.prepare()
    exoPlayer.playWhenReady = !isPause
    setWasAppInBackground(false)
}

private fun handleOnPause(
    exoPlayer: ExoPlayer,
    setWasAppInBackground: (Boolean) -> Unit,
) {
    exoPlayer.playWhenReady = false
    exoPlayer.pause()
    exoPlayer.stop()
    setWasAppInBackground(true)
}

private fun handleOnStop(
    exoPlayer: ExoPlayer,
    setWasAppInBackground: (Boolean) -> Unit,
) {
    exoPlayer.playWhenReady = false
    exoPlayer.pause()
    exoPlayer.stop()
    setWasAppInBackground(true)
}

private fun handleOnDestroy(exoPlayer: ExoPlayer) {
    exoPlayer.release()
}
