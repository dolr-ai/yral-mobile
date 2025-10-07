package com.yral.shared.libs.designsystem.windowInfo

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
actual fun rememberScreenFoldStateProvider(): ScreenFoldStateProvider {
    val context = LocalContext.current
    return koinInject { parametersOf(context) }
}
