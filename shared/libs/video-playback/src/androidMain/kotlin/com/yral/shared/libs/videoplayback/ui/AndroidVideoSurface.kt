@file:Suppress("MatchingDeclarationName")

package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle
import java.util.UUID

internal class AndroidVideoSurfaceHandle(
    val playerState: MutableState<Player?>,
    override val id: String = UUID.randomUUID().toString(),
) : VideoSurfaceHandle

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    contentScale: ContentScale,
    surfaceType: VideoSurfaceType,
    shutter: @Composable () -> Unit,
    onHandleReady: (VideoSurfaceHandle) -> Unit,
) {
    val playerState = remember { mutableStateOf<Player?>(null) }
    val handle = remember { AndroidVideoSurfaceHandle(playerState) }
    val surfaceTypeValue =
        when (surfaceType) {
            VideoSurfaceType.SurfaceView -> SURFACE_TYPE_SURFACE_VIEW
            VideoSurfaceType.TextureView -> SURFACE_TYPE_TEXTURE_VIEW
        }

    ContentFrame(
        modifier = modifier,
        player = playerState.value,
        surfaceType = surfaceTypeValue,
        contentScale = contentScale,
        shutter = shutter,
    )

    LaunchedEffect(handle) {
        onHandleReady(handle)
    }
}
