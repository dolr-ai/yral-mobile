package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.rust.service.utils.CanisterData

abstract class ConversationComponent {
    abstract val influencerId: String
    abstract val openProfile: (userCanisterData: CanisterData) -> Unit
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            influencerId: String,
            onBack: () -> Unit,
            openProfile: (userCanisterData: CanisterData) -> Unit,
        ): ConversationComponent =
            DefaultConversationComponent(
                componentContext = componentContext,
                influencerId = influencerId,
                onBack = onBack,
                openProfile = openProfile,
            )
    }
}
