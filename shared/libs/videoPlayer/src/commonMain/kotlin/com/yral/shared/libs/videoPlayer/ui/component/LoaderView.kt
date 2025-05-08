package com.yral.shared.libs.videoPlayer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.model.PlayerConfig

@Composable
internal fun LoaderView(playerConfig: PlayerConfig) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (playerConfig.loaderView != null) {
            playerConfig.loaderView?.invoke()
        } else {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .size(playerConfig.pauseResumeIconSize),
                color = playerConfig.loadingIndicatorColor,
            )
        }
    }
}
