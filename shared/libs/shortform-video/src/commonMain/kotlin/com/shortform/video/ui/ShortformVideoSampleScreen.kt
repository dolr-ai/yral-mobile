package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shortform.video.CoordinatorDeps
import com.shortform.video.MediaDescriptor

@Composable
fun ShortformVideoSampleScreen(
    items: List<MediaDescriptor>,
    modifier: Modifier = Modifier,
    deps: CoordinatorDeps = CoordinatorDeps(),
    overlay: @Composable (index: Int, item: MediaDescriptor) -> Unit = { _, _ -> },
) {
    val coordinator = rememberPlaybackCoordinatorWithLifecycle(deps)
    ShortformVideoFeed(
        items = items,
        coordinator = coordinator,
        modifier = modifier,
        overlay = overlay,
    )
}
