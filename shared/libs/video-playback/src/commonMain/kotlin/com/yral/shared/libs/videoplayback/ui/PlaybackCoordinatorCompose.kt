package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.PlaybackCoordinator

@Composable
expect fun rememberPlaybackCoordinator(deps: CoordinatorDeps = CoordinatorDeps()): PlaybackCoordinator
