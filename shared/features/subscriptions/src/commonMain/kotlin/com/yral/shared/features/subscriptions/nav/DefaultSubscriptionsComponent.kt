package com.yral.shared.features.subscriptions.nav

import com.arkivanov.decompose.ComponentContext

internal class DefaultSubscriptionsComponent(
    componentContext: ComponentContext,
    override val onBack: () -> Unit,
    override val onCreateVideo: () -> Unit,
    override val onExploreFeed: () -> Unit,
) : SubscriptionsComponent(),
    ComponentContext by componentContext
