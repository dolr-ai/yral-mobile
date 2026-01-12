package com.yral.shared.features.chat.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.chat.nav.wall.ChatWallComponent
import com.yral.shared.libs.arch.nav.HomeChildSnapshotProvider
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.serialization.Serializable

abstract class ChatComponent : HomeChildSnapshotProvider {
    abstract val requestLoginFactory: RequestLoginFactory
    abstract val stack: Value<ChildStack<*, Child>>

    abstract fun onBackClicked(): Boolean

    sealed class Child {
        class Wall(
            val component: ChatWallComponent,
        ) : Child()
        class Conversation(
            val component: ConversationComponent,
        ) : Child()
    }

    @Serializable
    data class Snapshot(
        val routes: List<Route>,
    ) {
        @Serializable
        sealed interface Route {
            @Serializable
            data object Wall : Route

            @Serializable
            data class Conversation(
                val influencerId: String,
                val influencerCategory: String = "",
                val influencerSource: InfluencerSource = InfluencerSource.CARD,
            ) : Route
        }
    }

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            requestLoginFactory: RequestLoginFactory,
            snapshot: Snapshot?,
            openProfile: (userCanisterData: CanisterData) -> Unit,
            openConversation: (
                influencerId: String,
                influencerCategory: String,
                influencerSource: InfluencerSource,
            ) -> Unit,
        ): ChatComponent =
            DefaultChatComponent(
                componentContext = componentContext,
                requestLoginFactory = requestLoginFactory,
                snapshot = snapshot,
                openProfile = openProfile,
                openConversation = openConversation,
            )
    }
}
