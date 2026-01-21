package com.yral.shared.features.subscriptions.nav.main

import com.arkivanov.decompose.ComponentContext

interface SubscriptionsMainComponent {
    val showBackIcon: Boolean
    val onBack: () -> Unit

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            showBackIcon: Boolean = false,
            onBack: () -> Unit = {},
        ): SubscriptionsMainComponent =
            DefaultSubscriptionsMainComponent(
                componentContext = componentContext,
                showBackIcon = showBackIcon,
                onBack = onBack,
            )
    }
}
