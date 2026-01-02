package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext

abstract class ChatWallComponent {
    abstract fun openConversation(
        influencerId: String,
        influencerCategory: String = "",
    )

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            openConversation: (String, String) -> Unit,
        ): ChatWallComponent =
            DefaultChatWallComponent(
                componentContext = componentContext,
                openConversation = openConversation,
            )
    }
}
