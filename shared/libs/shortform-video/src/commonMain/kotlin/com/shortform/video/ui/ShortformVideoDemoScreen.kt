package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shortform.video.CoordinatorDeps

@Composable
fun ShortformVideoDemoScreen(
    modifier: Modifier = Modifier,
    deps: CoordinatorDeps = CoordinatorDeps(),
) {
    ShortformVideoSampleScreen(
        items = SampleMedia.items,
        modifier = modifier,
        deps = deps,
    )
}
