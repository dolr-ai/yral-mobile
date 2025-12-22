package com.yral.shared.features.chat.nav.wall

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultChatWallComponent(
    componentContext: ComponentContext,
    private val openConversation: (String) -> Unit,
) : ChatWallComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun openConversation(userId: String) {
        openConversation.invoke(userId)
    }
}
