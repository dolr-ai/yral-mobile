package com.yral.android.ui.widgets.video

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.yral.android.R
import kotlinx.coroutines.delay
import java.io.File

@Suppress("LongMethod")
@OptIn(UnstableApi::class)
@Composable
fun YralVideoPlayer(
    modifier: Modifier = Modifier,
    url: String,
    autoPlay: Boolean = false,
    loop: Boolean = true,
    videoResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val exoPlayer =
        remember {
            createExoPlayer(context, url, loop) { error ->
                hasError = true
                onError(error)
            }
        }

    LaunchedEffect(exoPlayer) {
        val listener =
            createListener(
                setLoading = { isLoading = it },
                setPlaying = { isPlaying = it },
                setError = { error ->
                    if (error.isNotEmpty()) {
                        hasError = true
                        onError(error)
                    } else {
                        hasError = false
                    }
                },
            )
        exoPlayer?.addListener(listener)
    }

    LaunchedEffect(autoPlay) {
        exoPlayer?.playWhenReady = autoPlay
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    controllerShowTimeoutMs = 0
                    controllerHideOnTouch = true
                    post {
                        hideController()
                    }
                    player = exoPlayer
                    resizeMode = videoResizeMode
                }
            },
            modifier =
                Modifier
                    .wrapContentSize()
                    .align(Alignment.Center),
        )
        PlayerOverlay(
            modifier = Modifier.align(Alignment.Center),
            hasError = hasError,
            isLoading = isLoading,
            isPlaying = isPlaying,
            exoPlayer = exoPlayer,
        )
    }

    // Cleanup
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }
}

@Composable
private fun PlayerOverlay(
    modifier: Modifier,
    hasError: Boolean,
    isLoading: Boolean,
    isPlaying: Boolean,
    exoPlayer: ExoPlayer?,
) {
    Box(modifier.fillMaxSize()) {
        // Custom Play/Pause Control Overlay
        if (!hasError) {
            PlayPauseControl(isPlaying, isLoading) {
                if (isPlaying) {
                    exoPlayer?.pause()
                } else {
                    exoPlayer?.play()
                }
            }
        }

        // Error state - simplified without red background
        if (hasError) {
            PlayerError()
        }
    }
}

@Composable
private fun PlayerError() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.play),
            contentDescription = "Video Error - Tap to retry",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun PlayPauseControl(
    isPlaying: Boolean,
    isLoading: Boolean,
    togglePlayPause: () -> Unit,
) {
    var isPlayPauseVisible by remember { mutableStateOf(true) }
    LaunchedEffect(isPlayPauseVisible) {
        delay(PLAY_PAUSE_BUTTON_TIMEOUT)
        if (isPlayPauseVisible) {
            isPlayPauseVisible = false
        }
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable { isPlayPauseVisible = !isPlayPauseVisible },
        contentAlignment = Alignment.Center,
    ) {
        if (!isLoading && isPlayPauseVisible) {
            Image(
                painter =
                    painterResource(
                        if (isPlaying) R.drawable.pause else R.drawable.play,
                    ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { togglePlayPause() },
            )
        }
    }
}

private fun createListener(
    setLoading: (isLoading: Boolean) -> Unit,
    setPlaying: (isPlaying: Boolean) -> Unit,
    setError: (error: String) -> Unit,
): Player.Listener =
    object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    setLoading(true)
                }

                Player.STATE_READY -> {
                    setLoading(false)
                    setError("")
                }

                Player.STATE_ENDED -> {
                    setLoading(false)
                    setPlaying(false)
                }

                Player.STATE_IDLE -> {
                    setLoading(false)
                }
            }
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            setPlaying(playing)
        }

        override fun onPlayerError(error: PlaybackException) {
            setError("Playback error: ${error.message}")
            setLoading(false)
        }
    }

private fun createExoPlayer(
    context: Context,
    url: String,
    loop: Boolean,
    onError: (String) -> Unit,
): ExoPlayer? {
    val processedUrl =
        processUrl(url) ?: run {
            onError("Video file not found or invalid: $url")
            return null
        }
    val exoPlayer = ExoPlayer.Builder(context).build()
    val mediaItem = MediaItem.fromUri(processedUrl)
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    exoPlayer.prepare()
    return exoPlayer
}

private fun processUrl(url: String): String? =
    when {
        url.startsWith("http", ignoreCase = true) -> url
        else -> File(url).takeIf { it.exists() }?.toURI()?.toString()
    }

private const val PLAY_PAUSE_BUTTON_TIMEOUT = 2000L
