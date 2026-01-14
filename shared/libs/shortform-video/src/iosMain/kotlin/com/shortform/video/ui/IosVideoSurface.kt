package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.shortform.video.VideoSurfaceHandle
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVKit.AVPlayerViewController
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreGraphics.CGRectMake
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
    onHandleReady: (VideoSurfaceHandle) -> Unit,
) {
    var handle by remember { mutableStateOf<IosVideoSurfaceHandle?>(null) }

    UIKitView(
        factory = { PlayerViewContainer() },
        modifier = modifier,
        update = { view ->
            val container = view as PlayerViewContainer
            val current = handle ?: IosVideoSurfaceHandle(container.controller).also {
                handle = it
            }
            onHandleReady(current)
        },
        onReset = { view ->
            (view as? PlayerViewContainer)?.controller?.player = null
        },
        onRelease = { view ->
            (view as? PlayerViewContainer)?.controller?.player = null
        },
    )
}
