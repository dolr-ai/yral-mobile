package com.yral.shared.features.uploadvideo.nav.aiVideoGen

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator

abstract class AiVideoGenComponent {
    abstract val promptLogin: () -> Unit
    abstract val subscriptionCoordinator: SubscriptionCoordinator
    abstract fun onBack()

    abstract fun goToHome()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            goToHome: () -> Unit,
            onBack: () -> Unit,
            promptLogin: () -> Unit,
            subscriptionCoordinator: SubscriptionCoordinator,
        ): AiVideoGenComponent =
            DefaultAiVideoGenComponent(
                componentContext = componentContext,
                goToHome = goToHome,
                onBack = onBack,
                promptLogin = promptLogin,
                subscriptionCoordinator = subscriptionCoordinator,
            )
    }
}
