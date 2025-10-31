package com.yral.shared.features.wallet.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType
import org.koin.core.component.KoinComponent

internal class DefaultWalletComponent(
    componentContext: ComponentContext,
    override val showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
) : WalletComponent,
    ComponentContext by componentContext,
    KoinComponent
