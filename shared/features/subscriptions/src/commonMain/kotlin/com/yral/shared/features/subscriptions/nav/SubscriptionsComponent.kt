package com.yral.shared.features.subscriptions.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SubscriptionEntryPoint

abstract class SubscriptionsComponent {
    abstract val onBack: () -> Unit
    abstract val onCreateVideo: () -> Unit
    abstract val onExploreFeed: () -> Unit
    abstract val purchaseTimeMs: Long?
    abstract val entryPoint: SubscriptionEntryPoint

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            purchaseTimeMs: Long?,
            entryPoint: SubscriptionEntryPoint,
            onBack: () -> Unit = {},
            onCreateVideo: () -> Unit = {},
            onExploreFeed: () -> Unit = {},
        ): SubscriptionsComponent =
            DefaultSubscriptionsComponent(
                componentContext = componentContext,
                purchaseTimeMs = purchaseTimeMs,
                entryPoint = entryPoint,
                onBack = onBack,
                onCreateVideo = onCreateVideo,
                onExploreFeed = onExploreFeed,
            )
    }
}
