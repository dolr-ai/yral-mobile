package com.yral.android.ui.screens.wallet.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultWalletComponent(
    componentContext: ComponentContext,
) : WalletComponent,
    ComponentContext by componentContext,
    KoinComponent
