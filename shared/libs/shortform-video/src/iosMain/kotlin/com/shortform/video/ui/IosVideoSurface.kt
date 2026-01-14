package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.shortform.video.VideoSurfaceHandle
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView
import platform.UIKit.UIViewContentModeScaleAspectFill
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import java.util.UUID

internal class IosVideoSurfaceHandle(
    val containerView: UIView,
    val playerLayer: AVPlayerLayer,
    override val id: String = UUID.randomUUID().toString(),
) : VideoSurfaceHandle

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    onHandleReady: (VideoSurfaceHandle) -> Unit,
) {
    val handle = remember {
        val view = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
            contentMode = UIViewContentModeScaleAspectFill
            autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        }
        val layer = AVPlayerLayer().apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }
        view.layer.addSublayer(layer)
        IosVideoSurfaceHandle(view, layer)
    }

    UIKitView(
        factory = { handle.containerView },
        modifier = modifier,
        update = { view ->
            handle.playerLayer.frame = view.bounds
        },
    )

    LaunchedEffect(handle) {
        onHandleReady(handle)
    }
}
