package com.yral.shared.features.chat.nav.inbox

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.domain.models.OpenConversationParams

internal class DefaultInboxComponent(
    componentContext: ComponentContext,
    private val openConversation: (OpenConversationParams) -> Unit,
) : InboxComponent(),
    ComponentContext by componentContext {
    override fun openConversation(params: OpenConversationParams) {
        openConversation.invoke(params)
    }
}
