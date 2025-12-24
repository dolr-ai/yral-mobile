package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext

abstract class ChatWallComponent {
    abstract fun openConversation(influencerId: String)

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            openConversation: (String) -> Unit,
        ): ChatWallComponent =
            DefaultChatWallComponent(
                componentContext = componentContext,
                openConversation = openConversation,
            )
    }
}
