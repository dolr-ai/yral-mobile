@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.util

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.PlatformPlayerError
import com.yral.shared.libs.videoPlayer.model.ScreenResize

internal actual fun isDecoderInitFailed(error: PlatformPlayerError): Boolean =
    error.code == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED

@OptIn(UnstableApi::class)
@Composable
internal actual fun PlatformVideoPlayerView(
    modifier: Modifier,
    platformPlayer: PlatformPlayer?,
    screenResize: ScreenResize,
) {
    ContentFrame(
        modifier = modifier,
        player = platformPlayer?.internalExoPlayer,
        contentScale =
            when (screenResize) {
                ScreenResize.FIT -> ContentScale.Fit
                ScreenResize.FILL -> ContentScale.Crop
            },
        shutter = {},
    )
}
