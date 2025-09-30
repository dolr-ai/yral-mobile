package com.yral.android.ui.screens.wallet.nav

import com.arkivanov.decompose.ComponentContext

interface WalletComponent {
    companion object Companion {
        operator fun invoke(componentContext: ComponentContext): WalletComponent =
            DefaultWalletComponent(
                componentContext,
            )
    }
}
