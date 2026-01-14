package com.shortform.video.ui

import androidx.compose.runtime.Composable
import com.shortform.video.CoordinatorDeps
import com.shortform.video.PlaybackCoordinator

@Composable
expect fun rememberPlaybackCoordinator(
    deps: CoordinatorDeps = CoordinatorDeps(),
): PlaybackCoordinator
