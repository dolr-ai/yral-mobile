package com.yral.shared.features.wallet.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType

interface WalletComponent {
    val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
        ): WalletComponent =
            DefaultWalletComponent(
                componentContext,
                showAlertsOnDialog,
            )
    }
}
