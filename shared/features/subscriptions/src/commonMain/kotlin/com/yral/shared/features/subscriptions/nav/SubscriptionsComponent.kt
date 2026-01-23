package com.yral.shared.features.subscriptions.nav

import com.arkivanov.decompose.ComponentContext

abstract class SubscriptionsComponent {
    abstract val onBack: () -> Unit
    abstract val onCreateVideo: () -> Unit
    abstract val onExploreFeed: () -> Unit

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit = {},
            onCreateVideo: () -> Unit = {},
            onExploreFeed: () -> Unit = {},
        ): SubscriptionsComponent =
            DefaultSubscriptionsComponent(
                componentContext = componentContext,
                onBack = onBack,
                onCreateVideo = onCreateVideo,
                onExploreFeed = onExploreFeed,
            )
    }
}
