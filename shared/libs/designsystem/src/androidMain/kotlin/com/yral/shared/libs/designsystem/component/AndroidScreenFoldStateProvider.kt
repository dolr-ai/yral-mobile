package com.yral.shared.libs.designsystem.component

import android.content.Context
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.yral.shared.libs.designsystem.windowInfo.ScreenFoldStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AndroidScreenFoldStateProvider(
    context: Context,
) : ScreenFoldStateProvider {
    private val tracker = WindowInfoTracker.getOrCreate(context)

    override val isScreenUnfoldedFlow =
        tracker
            .windowLayoutInfo(context)
            .map { layoutInfo ->
                layoutInfo
                    .displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull()
                    ?.state == FoldingFeature.State.FLAT
            }.stateIn(
                scope = CoroutineScope(Dispatchers.Main),
                started = SharingStarted.Eagerly,
                initialValue = false,
            )
}
