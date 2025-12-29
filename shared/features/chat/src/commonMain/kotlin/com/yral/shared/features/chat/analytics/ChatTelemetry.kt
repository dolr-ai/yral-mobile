package com.yral.shared.features.chat.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AIMessageDeliveredEventData
import com.yral.shared.analytics.events.ChatInfluencerClickedEventData
import com.yral.shared.analytics.events.ChatSessionStartedEventData
import com.yral.shared.analytics.events.ChatTabViewedEventData
import com.yral.shared.analytics.events.InfluencerCardClickedEventData
import com.yral.shared.analytics.events.InfluencerCardsViewedEventData
import com.yral.shared.analytics.events.InfluencerChattedEventData
import com.yral.shared.analytics.events.InfluencerClickType
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.analytics.events.UserMessageSentEventData

class ChatTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun chatTabViewed(isLoggedIn: Boolean) {
        analyticsManager.trackEvent(ChatTabViewedEventData(isLoggedIn))
    }

    fun influencerCardsViewed(
        influencersShown: List<String>,
        totalCards: Int,
    ) {
        analyticsManager.trackEvent(
            InfluencerCardsViewedEventData(
                influencersShown = influencersShown.toTypedArray(),
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
        source: InfluencerSource,
    ) {
        analyticsManager.trackEvent(
            ChatInfluencerClickedEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                source = source,
            ),
        )
    }

    fun chatSessionStarted(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        source: InfluencerSource,
    ) {
        analyticsManager.trackEvent(
            ChatSessionStartedEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                chatSessionId = chatSessionId,
                source = source,
            ),
        )
    }

    fun userMessageSent(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        messageLength: Int,
        messageType: String,
    ) {
        analyticsManager.trackEvent(
            UserMessageSentEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                chatSessionId = chatSessionId,
                messageLength = messageLength,
                messageType = messageType,
            ),
        )
    }

    fun aiMessageDelivered(
        influencerId: String,
        influencerType: String,
        chatSessionId: String,
        responseLatencyMs: Int,
        responseLength: Int,
    ) {
        analyticsManager.trackEvent(
            AIMessageDeliveredEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                chatSessionId = chatSessionId,
                responseLatencyMs = responseLatencyMs,
                responseLength = responseLength,
            ),
        )
    }

    fun influencerChatted(
        influencerId: String,
        influencerType: String,
        date: String,
    ) {
        analyticsManager.trackEvent(
            InfluencerChattedEventData(
                influencerId = influencerId,
                influencerType = influencerType,
                date = date,
            ),
        )
    }
}
