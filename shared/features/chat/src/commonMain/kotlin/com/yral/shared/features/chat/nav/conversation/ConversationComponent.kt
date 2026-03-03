package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.rust.service.utils.CanisterData

abstract class ConversationComponent {
    abstract val requestLoginFactory: RequestLoginFactory
    abstract val subscriptionCoordinator: SubscriptionCoordinator
    abstract val openConversationParams: OpenConversationParams
    abstract val openProfile: (userCanisterData: CanisterData) -> Unit
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            requestLoginFactory: RequestLoginFactory,
            subscriptionCoordinator: SubscriptionCoordinator,
            openConversationParams: OpenConversationParams,
            onBack: () -> Unit,
            openProfile: (userCanisterData: CanisterData) -> Unit,
        ): ConversationComponent =
            DefaultConversationComponent(
                componentContext = componentContext,
                requestLoginFactory = requestLoginFactory,
                subscriptionCoordinator = subscriptionCoordinator,
                openConversationParams = openConversationParams,
                onBack = onBack,
                openProfile = openProfile,
            )
    }
}
