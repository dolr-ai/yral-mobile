package com.yral.shared.libs.designsystem.windowInfo

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

interface ScreenFoldStateProvider {
    val isScreenUnfoldedFlow: Flow<Boolean>
}

@Composable
expect fun rememberScreenFoldStateProvider(): ScreenFoldStateProvider
