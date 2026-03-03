package com.yral.shared.features.chat.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AIMessageDeliveredEventData
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.analytics.events.ChatInfluencerClickedEventData
import com.yral.shared.analytics.events.ChatSessionStartedEventData
import com.yral.shared.analytics.events.CreateBotCtaClickedEventData
import com.yral.shared.analytics.events.FreeAccessExpiredEventData
import com.yral.shared.analytics.events.InfluencerCardClickedEventData
import com.yral.shared.analytics.events.InfluencerCardsViewedEventData
import com.yral.shared.analytics.events.InfluencerClickType
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.analytics.events.SubscriptionClickedEventData
import com.yral.shared.analytics.events.SubscriptionFailedEventData
import com.yral.shared.analytics.events.SubscriptionSuccessEventData
import com.yral.shared.analytics.events.UserMessageSentEventData
import com.yral.shared.data.domain.models.ConversationInfluencerSource

class ChatTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun influencerCardsViewed(
        influencersShown: List<String>,
        totalCards: Int,
    ) {
        analyticsManager.trackEvent(
            InfluencerCardsViewedEventData(
                influencersShown = influencersShown,
                totalCards = totalCards,
            ),
        )
    }

    fun influencerCardClicked(
        influencerId: String,
        influencerType: String,
        clickType: InfluencerClickType,
        position: Int,
    ) {
        analyticsManager.trackEvent(
            InfluencerCardClickedEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                clickType = clickType,
                position = position,
            ),
        )
    }

    fun chatInfluencerClicked(
        influencerId: String,
        influencerType: String,
        source: ConversationInfluencerSource,
    ) {
        analyticsManager.trackEvent(
            ChatInfluencerClickedEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                source = source.toAnalyticsSource(),
            ),
        )
    }

    fun chatSessionStarted(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        source: ConversationInfluencerSource,
    ) {
        analyticsManager.trackEvent(
            ChatSessionStartedEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                chatSessionId = chatSessionId,
                source = source.toAnalyticsSource(),
            ),
        )
    }

    fun userMessageSent(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        messageLength: Int,
        messageType: String,
        message: String,
    ) {
        analyticsManager.trackEvent(
            UserMessageSentEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                chatSessionId = chatSessionId,
                messageLength = messageLength,
                messageType = messageType,
                message = message,
            ),
        )
    }

    fun aiMessageDelivered(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        responseLatencyMs: Int,
        responseLength: Int,
        message: String,
    ) {
        analyticsManager.trackEvent(
            AIMessageDeliveredEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                chatSessionId = chatSessionId,
                responseLatencyMs = responseLatencyMs,
                responseLength = responseLength,
                message = message,
            ),
        )
    }

    fun createBotCtaClicked(source: BotCreationSource) {
        analyticsManager.trackEvent(CreateBotCtaClickedEventData(source = source))
    }

    fun freeAccessExpired(botId: String) {
        analyticsManager.trackEvent(FreeAccessExpiredEventData(botId = botId))
    }

    fun subscriptionClicked(botId: String) {
        analyticsManager.trackEvent(SubscriptionClickedEventData(botId = botId))
    }

    fun subscriptionSuccess(botId: String) {
        analyticsManager.trackEvent(SubscriptionSuccessEventData(botId = botId))
    }

    fun subscriptionFailed(
        botId: String,
        reason: String,
    ) {
        analyticsManager.trackEvent(SubscriptionFailedEventData(botId = botId, reason = reason))
    }

    private fun ConversationInfluencerSource.toAnalyticsSource(): InfluencerSource =
        when (this) {
            ConversationInfluencerSource.CARD -> InfluencerSource.CARD
            ConversationInfluencerSource.PROFILE -> InfluencerSource.PROFILE
        }
}
