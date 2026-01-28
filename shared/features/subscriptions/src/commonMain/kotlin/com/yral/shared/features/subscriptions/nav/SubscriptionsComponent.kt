package com.yral.shared.features.subscriptions.nav

import com.arkivanov.decompose.ComponentContext

abstract class SubscriptionsComponent {
    abstract val onBack: () -> Unit
    abstract val onCreateVideo: () -> Unit
    abstract val onExploreFeed: () -> Unit
    abstract val validTill: Long?

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            validTill: Long?,
            onBack: () -> Unit = {},
            onCreateVideo: () -> Unit = {},
            onExploreFeed: () -> Unit = {},
        ): SubscriptionsComponent =
            DefaultSubscriptionsComponent(
                componentContext = componentContext,
                validTill = validTill,
                onBack = onBack,
                onCreateVideo = onCreateVideo,
                onExploreFeed = onExploreFeed,
            )
    }
}
