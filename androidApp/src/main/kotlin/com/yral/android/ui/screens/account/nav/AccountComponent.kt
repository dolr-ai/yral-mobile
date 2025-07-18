package com.yral.android.ui.screens.account.nav

import com.arkivanov.decompose.ComponentContext

interface AccountComponent {
    companion object Companion {
        operator fun invoke(componentContext: ComponentContext): AccountComponent =
            DefaultAccountComponent(
                componentContext,
            )
    }
}
