package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.rust.service.utils.CanisterData
import org.koin.core.component.KoinComponent

internal class DefaultConversationComponent(
    componentContext: ComponentContext,
    override val requestLoginFactory: RequestLoginFactory,
    override val subscriptionCoordinator: SubscriptionCoordinator,
    override val openConversationParams: OpenConversationParams,
    private val onBack: () -> Unit,
    override val openProfile: (userCanisterData: CanisterData) -> Unit,
    private val onSwitchToMainProfile: (onComplete: (Boolean) -> Unit) -> Unit,
) : ConversationComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }

    override fun switchToMainProfile(onComplete: (Boolean) -> Unit) {
        onSwitchToMainProfile.invoke(onComplete)
    }
}
