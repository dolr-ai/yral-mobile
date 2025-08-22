package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

private const val RENDER_PIXELS = 100
// private const val HAZE_INPUT_SCALE = 0.5f

@Composable
fun YralBlurredThumbnail(url: String) {
    // Enable haze effect to some other blur logic when want to use
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalContext.current)
                .data(url)
                .size(RENDER_PIXELS)
                .build(),
        modifier =
            Modifier
                // .hazeEffect { inputScale = HazeInputScale.Fixed(HAZE_INPUT_SCALE) }
                .fillMaxSize(),
        contentScale = ContentScale.Crop,
        contentDescription = "",
    )
}
