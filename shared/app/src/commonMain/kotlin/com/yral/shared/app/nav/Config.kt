package com.yral.shared.app.nav

import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.analytics.events.SubscriptionEntryPoint
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Config {
    @Serializable
    data object Splash : Config

    @Serializable
    data object Home : Config

    @Serializable
    data object EditProfile : Config

    @Serializable
    data class CreateInfluencer(
        val source: BotCreationSource,
    ) : Config

    @Serializable
    data class UserProfile(
        val userCanisterData: CanisterData,
    ) : Config

    @Serializable
    data class Conversation(
        val params: OpenConversationParams,
    ) : Config

    @Serializable
    data object Wallet : Config

    @Serializable
    data object CountrySelector : Config

    @Serializable
    data object OtpVerification : Config

    @Serializable
    data object MandatoryLogin : Config

    @Serializable
    data class Subscription(
        val purchaseTimeMs: Long?,
        val entryPoint: SubscriptionEntryPoint,
    ) : Config
}
