package com.yral.shared.features.chat.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.chat.nav.home.ChatHomeComponent
import com.yral.shared.features.chat.nav.home.ChatHomeComponent.InitialTab
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

internal class DefaultChatComponent(
    componentContext: ComponentContext,
    override val requestLoginFactory: RequestLoginFactory,
    override val subscriptionCoordinator: SubscriptionCoordinator,
    private val snapshot: Snapshot?,
    private val openProfile: (userCanisterData: CanisterData) -> Unit,
    private val openConversation: (OpenConversationParams) -> Unit,
    private val openCreateInfluencer: (source: BotCreationSource) -> Unit,
    private val initialTab: InitialTab = InitialTab.DISCOVER,
) : ChatComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialStack = {
                val saved = snapshot?.routes ?: emptyList()
                if (saved.isEmpty()) {
                    listOf(Config.Home)
                } else {
                    saved.map { it.toConfig() }
                }
            },
            handleBackButton = true,
            childFactory = ::child,
        )

    override fun onBackClicked(): Boolean {
        val items = stack.value.items
        return if (items.size > 1) {
            navigation.pop()
            true
        } else {
            false
        }
    }

    override fun openCreateInfluencer(source: BotCreationSource) {
        openCreateInfluencer.invoke(source)
    }

    override fun createHomeSnapshot(): Snapshot =
        Snapshot(
            routes =
                stack.value.items.map { item ->
                    when (val configuration = item.configuration) {
                        is Config.Home -> {
                            Snapshot.Route.Home
                        }

                        is Config.Conversation -> {
                            Snapshot.Route.Conversation(
                                params = configuration.params,
                            )
                        }

                        else -> {
                            Snapshot.Route.Home
                        }
                    }
                },
        )

    private fun Snapshot.Route.toConfig(): Config =
        when (this) {
            Snapshot.Route.Home -> {
                Config.Home
            }

            is Snapshot.Route.Conversation -> {
                Config.Conversation(params = params)
            }
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.Home -> Child.Home(chatHomeComponent(componentContext))
            is Config.Conversation -> Child.Conversation(conversationComponent(componentContext, config))
        }

    private fun chatHomeComponent(componentContext: ComponentContext): ChatHomeComponent =
        ChatHomeComponent.Companion(
            componentContext = componentContext,
            openConversation = openConversation,
            openCreateInfluencer = openCreateInfluencer,
            initialTab = initialTab,
        )

    private fun conversationComponent(
        componentContext: ComponentContext,
        config: Config.Conversation,
    ): ConversationComponent =
        ConversationComponent.Companion(
            componentContext = componentContext,
            requestLoginFactory = requestLoginFactory,
            subscriptionCoordinator = subscriptionCoordinator,
            openConversationParams = config.params,
            onBack = { navigation.pop() },
            openProfile = openProfile,
        )

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Home : Config

        @Serializable
        data class Conversation(
            val params: OpenConversationParams,
        ) : Config
    }
}
