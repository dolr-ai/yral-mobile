package com.yral.shared.features.leaderboard.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

@Composable
actual fun isScreenUnfolded(): Boolean {
    val context = LocalContext.current
    val windowLayoutInfoFlow =
        remember {
            WindowInfoTracker
                .getOrCreate(context)
                .windowLayoutInfo(context)
        }
    val windowLayoutInfo by windowLayoutInfoFlow.collectAsStateWithLifecycle(initialValue = null)
    val foldingFeature =
        windowLayoutInfo
            ?.displayFeatures
            ?.filterIsInstance<FoldingFeature>()
            ?.firstOrNull()
    return foldingFeature?.state == FoldingFeature.State.FLAT
}
