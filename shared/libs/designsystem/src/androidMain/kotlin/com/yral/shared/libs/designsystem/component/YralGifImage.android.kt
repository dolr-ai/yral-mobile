package com.yral.shared.libs.designsystem.component

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest

@Composable
internal actual fun YralGifImageFromBytes(
    bytes: ByteArray,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val context = LocalPlatformContext.current
    AsyncImage(
        model =
            ImageRequest
                .Builder(context)
                .data(bytes)
                .decoderFactory(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        AnimatedImageDecoder.Factory()
                    } else {
                        GifDecoder.Factory()
                    },
                ).build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}
