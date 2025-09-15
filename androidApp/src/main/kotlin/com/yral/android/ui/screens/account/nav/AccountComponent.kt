package com.yral.android.ui.screens.account.nav

import com.arkivanov.decompose.ComponentContext

interface AccountComponent {
    fun onBack()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit = {},
        ): AccountComponent =
            DefaultAccountComponent(
                componentContext,
                onBack = onBack,
            )
    }
}
