package com.yral.shared.features.chat.nav.inbox

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.domain.models.OpenConversationParams

@Suppress("UtilityClassWithPublicConstructor")
abstract class InboxComponent {
    abstract fun openConversation(params: OpenConversationParams)

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            openConversation: (OpenConversationParams) -> Unit,
        ): InboxComponent =
            DefaultInboxComponent(
                componentContext = componentContext,
                openConversation = openConversation,
            )
    }
}
