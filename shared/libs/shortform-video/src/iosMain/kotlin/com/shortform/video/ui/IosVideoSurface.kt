package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.viewinterop.UIKitView
import com.shortform.video.VideoSurfaceHandle
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVKit.AVPlayerViewController
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.timeControlStatus
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.CMTimeGetSeconds
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class IosVideoSurfaceHandle @OptIn(ExperimentalUuidApi::class) constructor(
    val controller: AVPlayerViewController,
    override val id: String = Uuid.random().toHexString(),
) : VideoSurfaceHandle

@OptIn(ExperimentalForeignApi::class)
private class PlayerViewContainer : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val controller: AVPlayerViewController =
        AVPlayerViewController().apply {
            showsPlaybackControls = false
            view.backgroundColor = UIColor.blackColor
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }

    init {
        backgroundColor = UIColor.blackColor
        controller.view.autoresizingMask =
            UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        controller.view.setFrame(bounds)
        addSubview(controller.view)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        controller.view.setFrame(bounds)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoSurface(
    modifier: Modifier,
    shutter: @Composable () -> Unit,
    onHandleReady: (VideoSurfaceHandle) -> Unit,
) {
    var handle by remember { mutableStateOf<IosVideoSurfaceHandle?>(null) }
    var showShutter by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        UIKitView(
            factory = { PlayerViewContainer() },
            modifier = Modifier.matchParentSize(),
            update = { view ->
                val container = view
                val current = handle ?: IosVideoSurfaceHandle(container.controller).also {
                    handle = it
                }
                onHandleReady(current)
            },
            onReset = { view ->
                view.controller.player = null
            },
            onRelease = { view ->
                view.controller.player = null
            },
        )

        if (showShutter) {
            shutter()
        }
    }

    val player = handle?.controller?.player
    DisposableEffect(player) {
        showShutter = true
        var lastItem: AVPlayerItem? = null
        val observer =
            player?.addPeriodicTimeObserverForInterval(
                interval = CMTimeMakeWithSeconds(0.2, 600),
                queue = null,
            ) {
                val item = player.currentItem
                if (item != lastItem) {
                    lastItem = item
                    showShutter = true
                }
                if (player.timeControlStatus == AVPlayerTimeControlStatusPlaying) {
                    val seconds = CMTimeGetSeconds(player.currentTime())
                    if (seconds > 0.01) {
                        showShutter = false
                    }
                }
            }
        onDispose {
            if (observer != null) {
                player.removeTimeObserver(observer)
            }
        }
    }
}
