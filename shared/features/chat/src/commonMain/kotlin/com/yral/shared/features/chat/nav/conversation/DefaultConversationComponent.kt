package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultConversationComponent(
    componentContext: ComponentContext,
    override val influencerId: String,
    private val onBack: () -> Unit,
) : ConversationComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }
}
