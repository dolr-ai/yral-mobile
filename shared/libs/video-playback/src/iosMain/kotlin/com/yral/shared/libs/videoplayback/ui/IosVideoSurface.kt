@file:Suppress("MatchingDeclarationName")

package com.yral.shared.libs.videoplayback.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitView
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.timeControlStatus
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class IosVideoSurfaceHandle
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        val controller: AVPlayerViewController,
        val playerState: MutableState<AVPlayer?>,
        override val id: String = Uuid.random().toHexString(),
    ) : VideoSurfaceHandle

@OptIn(ExperimentalForeignApi::class)
private class PlayerViewContainer(
    initialContentScale: ContentScale,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val controller: AVPlayerViewController =
        AVPlayerViewController().apply {
            showsPlaybackControls = false
            view.backgroundColor = UIColor.clearColor
            videoGravity = videoGravityFor(initialContentScale)
        }

    init {
        backgroundColor = UIColor.clearColor
        opaque = false
        clipsToBounds = true
        controller.view.autoresizingMask =
            UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        controller.view.backgroundColor = UIColor.clearColor
        controller.view.opaque = false
        controller.view.clipsToBounds = true
        controller.view.setFrame(bounds)
        addSubview(controller.view)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        controller.view.setFrame(bounds)
    }
}

@Suppress("LongMethod", "MagicNumber")
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoSurface(
    modifier: Modifier,
    contentScale: ContentScale,
    surfaceType: VideoSurfaceType,
    shutter: @Composable () -> Unit,
    onHandleReady: (VideoSurfaceHandle) -> Unit,
) {
    val playerState = remember { mutableStateOf<AVPlayer?>(null) }
    var handle by remember { mutableStateOf<IosVideoSurfaceHandle?>(null) }
    var showShutter by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        UIKitView(
            factory = { PlayerViewContainer(contentScale) },
            modifier = Modifier.matchParentSize(),
            update = { view ->
                val container = view
                val videoGravity = videoGravityFor(contentScale)
                if (container.controller.videoGravity != videoGravity) {
                    container.controller.videoGravity = videoGravity
                }
                if (handle == null) {
                    val current = IosVideoSurfaceHandle(container.controller, playerState)
                    handle = current
                    onHandleReady(current)
                }
            },
            onReset = { view ->
                view.controller.player = null
                playerState.value = null
                handle = null
            },
            onRelease = { view ->
                view.controller.player = null
                playerState.value = null
                handle = null
            },
        )

        if (showShutter || playerState.value == null) {
            shutter()
        }
    }

    val player = playerState.value
    DisposableEffect(player) {
        if (player == null) {
            showShutter = true
        }
        var lastItem: AVPlayerItem? = null
        val updateShutter = {
            player?.let { currentPlayer ->
                val item = currentPlayer.currentItem
                if (item != lastItem) {
                    lastItem = item
                    showShutter = true
                }
                if (currentPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying) {
                    val itemReady =
                        currentPlayer.currentItem?.status == AVPlayerItemStatusReadyToPlay
                    if (itemReady) {
                        showShutter = false
                    } else {
                        val seconds = CMTimeGetSeconds(currentPlayer.currentTime())
                        if (seconds > 0.01) {
                            showShutter = false
                        }
                    }
                }
            }
        }
        updateShutter()
        val observer =
            player?.addPeriodicTimeObserverForInterval(
                interval = CMTimeMakeWithSeconds(0.05, 600),
                queue = null,
            ) {
                updateShutter()
            }
        onDispose {
            if (observer != null) {
                player.removeTimeObserver(observer)
            }
        }
    }
}

private fun videoGravityFor(contentScale: ContentScale): String? =
    when (contentScale) {
        ContentScale.Crop -> AVLayerVideoGravityResizeAspectFill
        else -> AVLayerVideoGravityResizeAspect
    }
