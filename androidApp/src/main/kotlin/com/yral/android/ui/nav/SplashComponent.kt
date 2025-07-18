package com.yral.android.ui.nav

import com.arkivanov.decompose.ComponentContext

interface SplashComponent {
    companion object {
        operator fun invoke(componentContext: ComponentContext): SplashComponent =
            DefaultSplashComponent(
                componentContext,
            )
    }
}
