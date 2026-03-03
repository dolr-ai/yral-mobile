package com.yral.shared.features.chat.nav.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.chat.nav.inbox.InboxComponent
import com.yral.shared.features.chat.nav.wall.ChatWallComponent
import kotlinx.serialization.Serializable

internal class DefaultChatHomeComponent(
    componentContext: ComponentContext,
    private val openConversation: (OpenConversationParams) -> Unit,
    private val openCreateInfluencer: (source: BotCreationSource) -> Unit,
) : ChatHomeComponent(),
    ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Discover,
            handleBackButton = false,
            childFactory = ::child,
        )

    override fun onDiscoverTabClick() {
        navigation.replaceAll(Config.Discover)
    }

    override fun onInboxTabClick() {
        navigation.replaceAll(Config.Inbox)
    }

    override fun openCreateInfluencer() {
        openCreateInfluencer.invoke(BotCreationSource.CHAT_PAGE)
    }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.Discover -> Child.Discover(chatWallComponent(componentContext))
            Config.Inbox ->
                Child.Inbox(
                    InboxComponent(
                        componentContext = componentContext,
                        openConversation = openConversation,
                    ),
                )
        }

    private fun chatWallComponent(componentContext: ComponentContext): ChatWallComponent =
        ChatWallComponent.Companion(
            componentContext = componentContext,
            openConversation = openConversation,
            openCreateInfluencer = openCreateInfluencer,
        )

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Discover : Config

        @Serializable
        data object Inbox : Config
    }
}
