package com.yral.shared.features.subscriptions.nav

import com.arkivanov.decompose.ComponentContext

abstract class SubscriptionsComponent {
    abstract val onBack: () -> Unit
    abstract val onCreateVideo: () -> Unit
    abstract val onExploreFeed: () -> Unit
    abstract val purchaseTimeMs: Long?

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            purchaseTimeMs: Long?,
            onBack: () -> Unit = {},
            onCreateVideo: () -> Unit = {},
            onExploreFeed: () -> Unit = {},
        ): SubscriptionsComponent =
            DefaultSubscriptionsComponent(
                componentContext = componentContext,
                purchaseTimeMs = purchaseTimeMs,
                onBack = onBack,
                onCreateVideo = onCreateVideo,
                onExploreFeed = onExploreFeed,
            )
    }
}
