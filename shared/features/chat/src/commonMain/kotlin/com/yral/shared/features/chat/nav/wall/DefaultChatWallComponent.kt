package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.InfluencerSource
import org.koin.core.component.KoinComponent

internal class DefaultChatWallComponent(
    componentContext: ComponentContext,
    private val openConversation: (
        influencerId: String,
        influencerCategory: String,
        influencerSource: InfluencerSource,
    ) -> Unit,
) : ChatWallComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun openConversation(
        influencerId: String,
        influencerCategory: String,
        influencerSource: InfluencerSource,
    ) {
        openConversation.invoke(influencerId, influencerCategory, influencerSource)
    }
}
