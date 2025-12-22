package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext

abstract class ConversationComponent {
    abstract val userId: String
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            userId: String,
            onBack: () -> Unit,
        ): ConversationComponent =
            DefaultConversationComponent(
                componentContext = componentContext,
                userId = userId,
                onBack = onBack,
            )
    }
}
