package com.yral.shared.features.chat.ui.conversation

import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationBottomAreaStateTest {
    @Test
    fun `bot account state takes precedence over all other bottom area branches`() {
        assertEquals(
            ConversationBottomAreaState.BotAccountPrompt,
            resolveConversationBottomAreaState(
                isBotAccount = true,
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
                shouldShowInfluencerSubscriptionCard = false,
                shouldBlockChatNoProduct = false,
            ),
        )
    }
}
