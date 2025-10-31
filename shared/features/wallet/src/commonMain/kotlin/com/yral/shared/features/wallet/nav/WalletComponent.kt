package com.yral.shared.features.wallet.nav

import com.arkivanov.decompose.ComponentContext

interface WalletComponent {
    val showAlertsOnDialog: () -> Unit
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            showAlertsOnDialog: () -> Unit,
        ): WalletComponent =
            DefaultWalletComponent(
                componentContext,
                showAlertsOnDialog,
            )
    }
}
