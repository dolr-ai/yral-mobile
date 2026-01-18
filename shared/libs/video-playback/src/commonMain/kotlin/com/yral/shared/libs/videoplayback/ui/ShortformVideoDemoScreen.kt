package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoplayback.CoordinatorDeps

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
