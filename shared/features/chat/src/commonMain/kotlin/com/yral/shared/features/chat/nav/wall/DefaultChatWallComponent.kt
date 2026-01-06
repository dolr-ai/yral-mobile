package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultChatWallComponent(
    componentContext: ComponentContext,
    private val openConversation: (String, String) -> Unit,
) : ChatWallComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun openConversation(
        influencerId: String,
        influencerCategory: String,
    ) {
        openConversation.invoke(influencerId, influencerCategory)
    }
}
