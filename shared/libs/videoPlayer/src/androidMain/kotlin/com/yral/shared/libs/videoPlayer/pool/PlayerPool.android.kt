package com.yral.shared.libs.videoPlayer.pool

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.createHlsMediaSource
import com.yral.shared.libs.videoPlayer.createProgressiveMediaSource
import com.yral.shared.libs.videoPlayer.util.isHlsUrl

class AndroidPlatformPlayerFactory(
    private val context: Context,
) : PlatformPlayerFactory {
    override fun createPlayer(): PlatformPlayer = PlatformPlayer(createExoPlayer())

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(): ExoPlayer {
        val renderersFactory =
            DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setEnableDecoderFallback(true)

        return ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_OFF
                setHandleAudioBecomingNoisy(true)
                playWhenReady = false
                setForegroundMode(false)
            }
    }
}

class AndroidPlatformMediaSourceFactory(
    private val context: Context,
) : PlatformMediaSourceFactory {
    override fun createMediaSource(url: String): Any {
        val videoUri = url.toUri()
        val mediaItem = MediaItem.fromUri(videoUri)
        val mediaSource =
            if (isHlsUrl(url)) {
                createHlsMediaSource(mediaItem)
            } else {
                createProgressiveMediaSource(mediaItem, context)
            }

        return mediaSource
    }
}
