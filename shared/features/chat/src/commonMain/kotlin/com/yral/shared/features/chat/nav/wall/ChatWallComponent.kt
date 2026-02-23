package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.analytics.events.InfluencerSource

abstract class ChatWallComponent {
    abstract fun openConversation(
        influencerId: String,
        influencerCategory: String = "",
        influencerSource: InfluencerSource = InfluencerSource.CARD,
    )

    abstract fun openCreateInfluencer()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            openConversation: (
                influencerId: String,
                influencerCategory: String,
                influencerSource: InfluencerSource,
            ) -> Unit,
            openCreateInfluencer: (source: BotCreationSource) -> Unit,
        ): ChatWallComponent =
            DefaultChatWallComponent(
                componentContext = componentContext,
                openConversation = openConversation,
                openCreateInfluencer = openCreateInfluencer,
            )
    }
}
