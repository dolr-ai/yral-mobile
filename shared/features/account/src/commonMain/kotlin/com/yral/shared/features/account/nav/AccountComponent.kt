package com.yral.shared.features.account.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator

interface AccountComponent {
    val promptLogin: (pageName: SignupPageName) -> Unit
    val subscriptionCoordinator: SubscriptionCoordinator
    fun onBack()
    fun switchToMainProfile(onComplete: (Boolean) -> Unit = {})
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit = {},
            switchToMainProfile: (onComplete: (Boolean) -> Unit) -> Unit = {},
            promptLogin: (pageName: SignupPageName) -> Unit,
            subscriptionCoordinator: SubscriptionCoordinator,
        ): AccountComponent =
            DefaultAccountComponent(
                componentContext,
                onBack,
                switchToMainProfile,
                promptLogin,
                subscriptionCoordinator,
            )
    }
}
