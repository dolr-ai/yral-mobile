package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.data.domain.models.OpenConversationParams
import org.koin.core.component.KoinComponent

internal class DefaultChatWallComponent(
    componentContext: ComponentContext,
    private val openConversation: (OpenConversationParams) -> Unit,
    private val openCreateInfluencer: (source: BotCreationSource) -> Unit,
) : ChatWallComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun openConversation(params: OpenConversationParams) {
        openConversation.invoke(params)
    }

    override fun openCreateInfluencer() {
        openCreateInfluencer.invoke(BotCreationSource.CHAT_PAGE)
    }
}
