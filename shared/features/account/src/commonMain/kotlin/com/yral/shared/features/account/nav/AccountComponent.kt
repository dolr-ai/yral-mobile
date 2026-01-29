package com.yral.shared.features.account.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator

interface AccountComponent {
    val promptLogin: (pageName: SignupPageName) -> Unit
    val subscriptionCoordinator: SubscriptionCoordinator
    fun onBack()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit = {},
            promptLogin: (pageName: SignupPageName) -> Unit,
            subscriptionCoordinator: SubscriptionCoordinator,
        ): AccountComponent =
            DefaultAccountComponent(
                componentContext,
                onBack,
                promptLogin,
                subscriptionCoordinator,
            )
    }
}
