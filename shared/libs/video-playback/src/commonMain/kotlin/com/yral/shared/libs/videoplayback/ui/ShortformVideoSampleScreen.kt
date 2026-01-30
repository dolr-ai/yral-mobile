package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.MediaDescriptor

@Composable
fun ShortformVideoSampleScreen(
    items: List<MediaDescriptor>,
    modifier: Modifier = Modifier,
    deps: CoordinatorDeps = CoordinatorDeps(),
    overlay: @Composable (index: Int, item: MediaDescriptor) -> Unit = { _, _ -> },
) {
    KeepScreenOnEffect(true)
    val coordinator = rememberPlaybackCoordinatorWithLifecycle(deps)
    VideoFeed(
        items = items,
        coordinator = coordinator,
        modifier = modifier,
        overlay = overlay,
    )
}
