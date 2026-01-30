package com.yral.shared.features.account.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import org.koin.core.component.KoinComponent

internal class DefaultAccountComponent(
    componentContext: ComponentContext,
    val onBack: () -> Unit = {},
    override val promptLogin: (pageName: SignupPageName) -> Unit,
    override val subscriptionCoordinator: SubscriptionCoordinator,
) : AccountComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }
}
