package com.yral.shared.features.chat.nav.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.chat.nav.inbox.InboxComponent
import com.yral.shared.features.chat.nav.wall.ChatWallComponent
import com.yral.shared.koin.koinInstance
import kotlinx.serialization.Serializable

internal class DefaultChatHomeComponent(
    componentContext: ComponentContext,
    private val openConversation: (OpenConversationParams) -> Unit,
    private val openCreateInfluencer: (source: BotCreationSource) -> Unit,
    private val initialTab: InitialTab = InitialTab.DISCOVER,
) : ChatHomeComponent(),
    ComponentContext by componentContext {
    private val sessionManager: SessionManager = koinInstance.get()

    private class InitialConfigHolder : InstanceKeeper.Instance {
        var config: Config = Config.Discover
    }

    private val initialConfigHolder: InitialConfigHolder =
        instanceKeeper.getOrCreate("initialConfig") {
            InitialConfigHolder().also { holder ->
                holder.config =
                    when {
                        sessionManager.isBotAccount == true -> Config.Inbox
                        initialTab == InitialTab.INBOX -> Config.Inbox
                        else -> Config.Discover
                    }
            }
        }

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = initialConfigHolder.config,
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
            Config.Discover -> {
                Child.Discover(chatWallComponent(componentContext))
            }

            Config.Inbox -> {
                Child.Inbox(
                    InboxComponent(
                        componentContext = componentContext,
                        openConversation = openConversation,
                    ),
                )
            }
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
