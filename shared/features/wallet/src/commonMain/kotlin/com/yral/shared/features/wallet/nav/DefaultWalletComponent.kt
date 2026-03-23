package com.yral.shared.features.wallet.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.rust.service.utils.CanisterData
import org.koin.core.component.KoinComponent

internal class DefaultWalletComponent(
    componentContext: ComponentContext,
    override val showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
    override val showBackIcon: Boolean,
    override val onBack: () -> Unit,
    override val onCreateInfluencer: () -> Unit,
    override val onOpenProfile: (CanisterData) -> Unit,
) : WalletComponent,
    ComponentContext by componentContext,
    KoinComponent
