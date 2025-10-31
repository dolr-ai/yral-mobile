package com.yral.shared.features.wallet.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultWalletComponent(
    componentContext: ComponentContext,
    override val showAlertsOnDialog: () -> Unit,
) : WalletComponent,
    ComponentContext by componentContext,
    KoinComponent
