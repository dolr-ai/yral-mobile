@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.util

import android.view.TextureView
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
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
    val player = platformPlayer?.internalExoPlayer

    // Use a FrameLayout container so we can add/remove the TextureView dynamically
    // This allows the TextureView to survive composable recreation via VideoSurfaceManager
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { container ->
            // Get or create the TextureView for this player
            if (player != null) {
                val textureView = VideoSurfaceManager.getTextureViewForPlayer(container.context, player)

                // Only add if not already a child of this container
                if (textureView.parent != container) {
                    // Remove from any previous parent
                    (textureView.parent as? FrameLayout)?.removeView(textureView)
                    // Add to this container
                    container.removeAllViews()
                    container.addView(textureView)
                }
            } else {
                // No player, clear the container
                container.removeAllViews()
            }
        },
    )
}
