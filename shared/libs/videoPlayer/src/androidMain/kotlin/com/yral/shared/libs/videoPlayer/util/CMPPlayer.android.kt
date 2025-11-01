@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.util

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.PlatformPlayerError
import com.yral.shared.libs.videoPlayer.model.ScreenResize

internal actual fun isDecoderInitFailed(error: PlatformPlayerError): Boolean =
    error.code == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED

@Composable
internal actual fun PlatformVideoPlayerView(
    modifier: Modifier,
    platformPlayer: PlatformPlayer?,
    screenResize: ScreenResize,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context)
                .apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FILL
                }
        },
        modifier = modifier,
        update = { playerView ->
            playerView.player = platformPlayer?.internalExoPlayer
            playerView.keepScreenOn = true
            playerView.resizeMode =
                when (screenResize) {
                    ScreenResize.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ScreenResize.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
        },
        onReset = { playerView ->
            playerView.keepScreenOn = false
            playerView.player = null
        },
        onRelease = { playerView ->
            playerView.keepScreenOn = false
            playerView.player = null
        },
    )
}
