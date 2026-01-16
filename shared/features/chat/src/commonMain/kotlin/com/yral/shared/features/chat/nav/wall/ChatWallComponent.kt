package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.InfluencerSource

abstract class ChatWallComponent {
    abstract fun openConversation(
        influencerId: String,
        influencerCategory: String = "",
        influencerSource: InfluencerSource = InfluencerSource.CARD,
    )

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            openConversation: (
                influencerId: String,
                influencerCategory: String,
                influencerSource: InfluencerSource,
            ) -> Unit,
        ): ChatWallComponent =
            DefaultChatWallComponent(
                componentContext = componentContext,
                openConversation = openConversation,
            )
    }
}
