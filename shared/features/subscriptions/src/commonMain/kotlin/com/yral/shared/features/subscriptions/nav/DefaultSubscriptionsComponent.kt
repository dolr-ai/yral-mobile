package com.yral.shared.features.subscriptions.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SubscriptionEntryPoint

internal class DefaultSubscriptionsComponent(
    componentContext: ComponentContext,
    override val purchaseTimeMs: Long?,
    override val entryPoint: SubscriptionEntryPoint,
    override val onBack: () -> Unit,
    override val onCreateVideo: () -> Unit,
    override val onExploreFeed: () -> Unit,
) : SubscriptionsComponent(),
    ComponentContext by componentContext
