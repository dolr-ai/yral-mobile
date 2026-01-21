package com.yral.shared.features.subscriptions.nav.main

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultSubscriptionsMainComponent(
    componentContext: ComponentContext,
    override val showBackIcon: Boolean,
    override val onBack: () -> Unit,
) : SubscriptionsMainComponent,
    ComponentContext by componentContext,
    KoinComponent
