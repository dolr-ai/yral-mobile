package com.yral.shared.libs.designsystem.windowInfo

import androidx.compose.runtime.Composable
import org.koin.compose.koinInject

@Composable
actual fun rememberScreenFoldStateProvider(): ScreenFoldStateProvider = koinInject()
