package com.yral.shared.features.wallet.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType

interface WalletComponent {
    val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    val showBackIcon: Boolean
    val onBack: () -> Unit
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
            showBackIcon: Boolean = false,
            onBack: () -> Unit = {},
        ): WalletComponent =
            DefaultWalletComponent(
                componentContext,
                showAlertsOnDialog,
                showBackIcon,
                onBack,
            )
    }
}
