@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.cValue
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerActionAtItemEndNone
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeErrorKey
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.actionAtItemEnd
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTime
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.dispatch_get_main_queue
import kotlin.math.roundToLong

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
actual fun YralVideoPlayer(
    modifier: Modifier,
    url: String,
    autoPlay: Boolean,
    loop: Boolean,
    videoResizeMode: ResizeMode,
    onError: (String) -> Unit,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val processedUrl = remember(url) { processUrl(url) }

    val playerHolder =
        remember(processedUrl) {
            processedUrl?.let {
                IosAudioSession.ensurePlaybackSessionActive()
                PlayerHolder(
                    player =
                        AVPlayer(uRL = it).apply {
                            actionAtItemEnd = AVPlayerActionAtItemEndNone
                        },
                    viewController =
                        AVPlayerViewController().apply {
                            showsPlaybackControls = false
                            view.backgroundColor = UIColor.blackColor
                        },
                )
            }
        }

    val player = playerHolder?.player

    LaunchedEffect(url, processedUrl) {
        if (processedUrl == null) {
            hasError = true
            isLoading = false
            onError("Video file not found or invalid: $url")
        } else {
            hasError = false
            isLoading = true
        }
    }

    LaunchedEffect(player, autoPlay) {
        if (player == null) return@LaunchedEffect
        if (autoPlay) {
            player.play()
        } else {
            player.pause()
            isPlaying = false
        }
    }

    DisposableEffect(player) {
        if (player == null) {
            return@DisposableEffect onDispose { }
        }

        @Suppress("MagicNumber")
        val interval = cmTimeInterval(0.25)
        val observerToken =
            player.addPeriodicTimeObserverForInterval(
                interval = interval,
                queue = dispatch_get_main_queue(),
            ) { _: CValue<CMTime> ->
                val currentItem = player.currentItem
                when (currentItem?.status) {
                    AVPlayerItemStatusReadyToPlay -> {
                        if (isLoading) {
                            isLoading = false
                        }
                        if (hasError) {
                            hasError = false
                        }
                    }

                    AVPlayerItemStatusFailed -> {
                        val errorMessage =
                            currentItem.error?.localizedDescription
                                ?: "Playback error"
                        if (!hasError) {
                            hasError = true
                            onError(errorMessage)
                        }
                        isLoading = false
                        isPlaying = false
                    }

                    else -> {
                        isLoading = true
                    }
                }

                val playing =
                    player.timeControlStatus == AVPlayerTimeControlStatusPlaying &&
                        !hasError &&
                        !isLoading
                if (isPlaying != playing) {
                    isPlaying = playing
                }

                if (player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate) {
                    isLoading = true
                }
            }

        onDispose {
            player.removeTimeObserver(observerToken)
        }
    }

    DisposableEffect(player, loop) {
        if (player == null) {
            return@DisposableEffect onDispose { }
        }

        val notificationCenter = NSNotificationCenter.defaultCenter
        val currentItem = player.currentItem
        val endObserver =
            currentItem?.let {
                notificationCenter.addObserverForName(
                    name = AVPlayerItemDidPlayToEndTimeNotification,
                    `object` = it,
                    queue = NSOperationQueue.mainQueue(),
                ) { _: NSNotification? ->
                    if (loop) {
                        player.seekToTime(cmTimeZero())
                        player.play()
                    } else {
                        player.seekToTime(cmTimeZero())
                        isPlaying = false
                    }
                }
            }
        val failureObserver =
            currentItem?.let {
                notificationCenter.addObserverForName(
                    name = AVPlayerItemFailedToPlayToEndTimeNotification,
                    `object` = it,
                    queue = NSOperationQueue.mainQueue(),
                ) { notification: NSNotification? ->
                    val avError = notification?.userInfo?.get(AVPlayerItemFailedToPlayToEndTimeErrorKey)
                    val errorDescription =
                        ((avError as? NSError)?.localizedDescription)
                            ?: player.currentItem?.error?.localizedDescription
                            ?: "Playback error"
                    hasError = true
                    isLoading = false
                    onError(errorDescription)
                }
            }

        onDispose {
            if (endObserver != null) {
                notificationCenter.removeObserver(endObserver)
            }
            if (failureObserver != null) {
                notificationCenter.removeObserver(failureObserver)
            }
        }
    }

    val holder = playerHolder

    DisposableEffect(holder) {
        onDispose {
            holder?.dispose()
        }
    }

    Box(modifier = modifier) {
        holder?.let { currentHolder ->
            UIKitView(
                factory = {
                    currentHolder.viewController.player = currentHolder.player
                    currentHolder.viewController.videoGravity = videoResizeMode.toAVLayerVideoGravity()
                    currentHolder.viewController.view
                },
                update = { _: UIView ->
                    currentHolder.viewController.player = currentHolder.player
                    currentHolder.viewController.videoGravity = videoResizeMode.toAVLayerVideoGravity()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        PlayerOverlay(
            modifier = Modifier.align(Alignment.Center),
            hasError = hasError,
            isLoading = isLoading,
            isPlaying = isPlaying,
            togglePlayPause = {
                val currentPlayer = holder?.player ?: return@PlayerOverlay
                if (hasError) return@PlayerOverlay
                if (isPlaying) {
                    currentPlayer.pause()
                    isPlaying = false
                } else {
                    currentPlayer.play()
                    isPlaying = true
                }
            },
        )
    }
}

private fun ResizeMode.toAVLayerVideoGravity(): String? =
    when (this) {
        ResizeMode.FIT -> AVLayerVideoGravityResizeAspect
        ResizeMode.FIXED_WIDTH -> AVLayerVideoGravityResizeAspectFill
    }

private fun processUrl(rawUrl: String): NSURL? {
    val url = rawUrl.trim()
    if (url.isEmpty()) return null

    return when {
        url.startsWith("http", ignoreCase = true) -> NSURL(string = url)
        url.startsWith("file://") -> NSURL(string = url)
        NSFileManager.defaultManager.fileExistsAtPath(path = url) -> NSURL.fileURLWithPath(path = url)
        else -> null
    }
}

private const val TIME_SCALE = 600L

@Suppress("MagicNumber")
private fun cmTimeInterval(seconds: Double): CValue<CMTime> =
    cValue {
        value = (seconds * TIME_SCALE).roundToLong()
        timescale = TIME_SCALE.toInt()
        flags = 1u
        epoch = 0L
    }

@Suppress("MagicNumber")
private fun cmTimeZero(): CValue<CMTime> =
    cValue {
        value = 0L
        timescale = TIME_SCALE.toInt()
        flags = 1u
        epoch = 0L
    }

private class PlayerHolder(
    val player: AVPlayer,
    val viewController: AVPlayerViewController,
) {
    fun dispose() {
        viewController.player = null
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }
}
