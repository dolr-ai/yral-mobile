package com.yral.android.ui.screens.uploadVideo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.yral.android.ui.widgets.video.YralLocalVideoPlayer

@Composable
fun VideoView(
    modifier: Modifier,
    videoFilePath: String,
) {
    YralLocalVideoPlayer(
        modifier = modifier,
        localFilePath = videoFilePath,
        autoPlay = true,
        onError = { error ->
            Logger.d("Video error: $error")
        },
    )
}
