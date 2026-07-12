package com.yral.shared.features.chat.ui.conversation

import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationBottomAreaStateTest {
    @Test
    fun `bot account with bot participant shows switch profile prompt`() {
        assertEquals(
            ConversationBottomAreaState.BotAccountPrompt,
            resolveConversationBottomAreaState(
                isBotAccount = true,
                isHumanParticipantConversation = false,
                shouldShowInfluencerSubscriptionCard = true,
                shouldBlockChatNoProduct = true,
            ),
        )
    }

    @Test
    fun `bot account with human participant shows read only prompt`() {
        assertEquals(
            ConversationBottomAreaState.BotAccountReadOnly,
            resolveConversationBottomAreaState(
                isBotAccount = true,
                isHumanParticipantConversation = true,
                shouldShowInfluencerSubscriptionCard = true,
                shouldBlockChatNoProduct = true,
            ),
        )
    }

    @Test
    fun `subscription card state is selected when purchase is available`() {
        assertEquals(
            ConversationBottomAreaState.InfluencerSubscription,
            resolveConversationBottomAreaState(
                isBotAccount = false,
                isHumanParticipantConversation = true,
                shouldShowInfluencerSubscriptionCard = true,
                shouldBlockChatNoProduct = false,
            ),
        )
    }

    @Test
    fun `subscription unavailable state is selected when no product can be purchased`() {
        assertEquals(
            ConversationBottomAreaState.SubscriptionUnavailable,
            resolveConversationBottomAreaState(
                isBotAccount = false,
                isHumanParticipantConversation = true,
                shouldShowInfluencerSubscriptionCard = false,
                shouldBlockChatNoProduct = true,
            ),
        )
    }

    @Test
    fun `chat input is the default bottom area state`() {
        assertEquals(
            ConversationBottomAreaState.ChatInput,
            resolveConversationBottomAreaState(
                isBotAccount = false,
                isHumanParticipantConversation = true,
                shouldShowInfluencerSubscriptionCard = false,
                shouldBlockChatNoProduct = false,
            ),
        )
    }

    @Test
    fun `bot account profile target uses human participant when present`() {
        assertEquals(
            "human-principal",
            resolveConversationProfilePrincipal(
                isBotAccount = true,
                humanParticipantUserId = "human-principal",
                influencerId = "bot-principal",
            ),
        )
    }

    @Test
    fun `bot account profile target uses influencer when human participant is absent`() {
        assertEquals(
            "bot-principal",
            resolveConversationProfilePrincipal(
                isBotAccount = true,
                humanParticipantUserId = null,
                influencerId = "bot-principal",
            ),
        )
    }

    @Test
    fun `human account profile target uses influencer even when human participant is present`() {
        assertEquals(
            "bot-principal",
            resolveConversationProfilePrincipal(
                isBotAccount = false,
                humanParticipantUserId = "human-principal",
                influencerId = "bot-principal",
            ),
        )
    }
}
