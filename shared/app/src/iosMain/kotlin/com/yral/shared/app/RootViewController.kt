package com.yral.shared.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.ui.MyApplicationTheme
import com.yral.shared.app.ui.screens.RootScreen
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import platform.UIKit.UIViewController

@OptIn(ExperimentalMaterial3Api::class)
fun rootViewController(rootComponent: RootComponent): UIViewController =
    ComposeUIViewController {
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
                MyApplicationTheme {
                    RootScreen(rootComponent)
                }
            }
        }
    }
