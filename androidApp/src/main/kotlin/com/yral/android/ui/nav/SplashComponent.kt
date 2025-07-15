package com.yral.android.ui.nav

import com.arkivanov.decompose.ComponentContext

interface SplashComponent {
    companion object {
        @Suppress("MaxLineLength")
        operator fun invoke(componentContext: ComponentContext): SplashComponent = DefaultSplashComponent(componentContext)
    }
}
