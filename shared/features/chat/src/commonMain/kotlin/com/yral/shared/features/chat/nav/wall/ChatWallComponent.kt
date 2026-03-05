package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.data.domain.models.OpenConversationParams

abstract class ChatWallComponent {
    abstract fun openConversation(params: OpenConversationParams)

    abstract fun openCreateInfluencer()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            openConversation: (OpenConversationParams) -> Unit,
            openCreateInfluencer: (source: BotCreationSource) -> Unit,
        ): ChatWallComponent =
            DefaultChatWallComponent(
                componentContext = componentContext,
                openConversation = openConversation,
                openCreateInfluencer = openCreateInfluencer,
            )
    }
}
