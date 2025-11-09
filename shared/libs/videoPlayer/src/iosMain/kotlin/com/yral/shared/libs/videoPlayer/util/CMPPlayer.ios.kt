package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.PlatformPlayerError
import com.yral.shared.libs.videoPlayer.model.ScreenResize
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth

internal actual fun isDecoderInitFailed(error: PlatformPlayerError): Boolean = false

@Composable
internal actual fun PlatformVideoPlayerView(
    modifier: Modifier,
    platformPlayer: PlatformPlayer?,
    screenResize: ScreenResize,
) {
    UIKitView(
        factory = {
            PlayerViewContainer().apply {
                controller.player = platformPlayer?.nativePlayer
                controller.videoGravity = screenResize.toAVLayerVideoGravity()
            }
        },
        modifier = modifier,
        update = { view: UIView ->
            (view as? PlayerViewContainer)?.let { container ->
                container.controller.player = platformPlayer?.nativePlayer
                container.controller.videoGravity = screenResize.toAVLayerVideoGravity()
            }
        },
        onReset = { view: UIView ->
            (view as? PlayerViewContainer)?.clearPlayer()
        },
        onRelease = { view: UIView ->
            (view as? PlayerViewContainer)?.clearPlayer()
        },
    )
}

private fun ScreenResize.toAVLayerVideoGravity(): String? =
    when (this) {
        ScreenResize.FIT -> AVLayerVideoGravityResizeAspect
        ScreenResize.FILL -> AVLayerVideoGravityResizeAspectFill
    }

private fun PlayerViewContainer.clearPlayer() {
    controller.player = null
}

@OptIn(ExperimentalForeignApi::class)
private class PlayerViewContainer : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val controller: AVPlayerViewController =
        AVPlayerViewController().apply {
            showsPlaybackControls = false
            view.backgroundColor = UIColor.blackColor
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
