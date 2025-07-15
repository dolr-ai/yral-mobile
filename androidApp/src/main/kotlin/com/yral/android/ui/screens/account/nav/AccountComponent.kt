package com.yral.android.ui.screens.account.nav

import com.arkivanov.decompose.ComponentContext

interface AccountComponent {
    companion object Companion {
        @Suppress("MaxLineLength")
        operator fun invoke(componentContext: ComponentContext): AccountComponent = DefaultAccountComponent(componentContext)
    }
}
