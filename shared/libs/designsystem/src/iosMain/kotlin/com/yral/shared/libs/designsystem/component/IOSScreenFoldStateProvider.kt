package com.yral.shared.libs.designsystem.component

import com.yral.shared.libs.designsystem.windowInfo.ScreenFoldStateProvider
import kotlinx.coroutines.flow.flowOf

class IOSScreenFoldStateProvider : ScreenFoldStateProvider {
    override val isScreenUnfoldedFlow = flowOf(false)
}
