package com.yral.shared.features.chat.nav.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.chat.nav.inbox.InboxComponent
import com.yral.shared.features.chat.nav.wall.ChatWallComponent

abstract class ChatHomeComponent {
    abstract val stack: Value<ChildStack<*, Child>>

    abstract fun onDiscoverTabClick()
    abstract fun onInboxTabClick()
    abstract fun openCreateInfluencer()
    sealed class Child {
        class Discover(
            val component: ChatWallComponent,
        ) : Child()

        class Inbox(
            val component: InboxComponent,
        ) : Child()
    }

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            openConversation: (OpenConversationParams) -> Unit,
            openCreateInfluencer: (source: BotCreationSource) -> Unit,
        ): ChatHomeComponent =
            DefaultChatHomeComponent(
                componentContext = componentContext,
                openConversation = openConversation,
                openCreateInfluencer = openCreateInfluencer,
            )
    }
}
