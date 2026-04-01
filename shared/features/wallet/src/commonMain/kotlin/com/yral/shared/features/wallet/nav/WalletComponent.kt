package com.yral.shared.features.wallet.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.rust.service.utils.CanisterData

interface WalletComponent {
    val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    val showBackIcon: Boolean
    val onBack: () -> Unit
    val onCreateInfluencer: () -> Unit
    val onSwitchProfile: () -> Unit
    val onOpenProfile: (CanisterData) -> Unit
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
            showBackIcon: Boolean = false,
            onBack: () -> Unit = {},
            onCreateInfluencer: () -> Unit = {},
            onSwitchProfile: () -> Unit = {},
            onOpenProfile: (CanisterData) -> Unit = {},
        ): WalletComponent =
            DefaultWalletComponent(
                componentContext,
                showAlertsOnDialog,
                showBackIcon,
                onBack,
                onCreateInfluencer,
                onSwitchProfile,
                onOpenProfile,
            )
    }
}
