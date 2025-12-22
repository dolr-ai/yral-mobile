package com.yral.shared.features.chat.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.chat.nav.wall.ChatWallComponent
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

internal class DefaultChatComponent(
    componentContext: ComponentContext,
    private val snapshot: Snapshot?,
    private val openProfile: (userCanisterData: CanisterData) -> Unit,
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
                    listOf(Config.Wall)
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

    override fun createHomeSnapshot(): Snapshot =
        Snapshot(
            routes =
                stack.value.items.map { item ->
                    when (val configuration = item.configuration) {
                        is Config.Wall -> Snapshot.Route.Wall
                        is Config.Conversation ->
                            Snapshot.Route.Conversation(
                                influencerId = configuration.influencerId,
                            )
                        else -> Snapshot.Route.Wall
                    }
                },
        )

    private fun Snapshot.Route.toConfig(): Config =
        when (this) {
            Snapshot.Route.Wall -> Config.Wall
            is Snapshot.Route.Conversation ->
                Config.Conversation(influencerId = influencerId)
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.Wall -> Child.Wall(chatWallComponent(componentContext))
            is Config.Conversation -> Child.Conversation(conversationComponent(componentContext, config))
        }

    private fun chatWallComponent(componentContext: ComponentContext): ChatWallComponent =
        ChatWallComponent.Companion(
            componentContext = componentContext,
            openConversation = { influencerId ->
                navigation.pushNew(Config.Conversation(influencerId = influencerId))
            },
        )

    private fun conversationComponent(
        componentContext: ComponentContext,
        config: Config.Conversation,
    ): ConversationComponent =
        ConversationComponent.Companion(
            componentContext = componentContext,
            influencerId = config.influencerId,
            onBack = { navigation.pop() },
            openProfile = openProfile,
        )

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Wall : Config

        @Serializable
        data class Conversation(
            val influencerId: String,
        ) : Config
    }
}
