package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext

abstract class ConversationComponent {
    abstract val influencerId: String
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            influencerId: String,
            onBack: () -> Unit,
        ): ConversationComponent =
            DefaultConversationComponent(
                componentContext = componentContext,
                influencerId = influencerId,
                onBack = onBack,
            )
    }
}
