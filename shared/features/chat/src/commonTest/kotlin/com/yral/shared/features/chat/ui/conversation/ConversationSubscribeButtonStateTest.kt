package com.yral.shared.features.chat.ui.conversation

import com.yral.shared.features.chat.viewmodel.ConversationViewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationSubscribeButtonStateTest {
    @Test
    fun `verified purchases keep the header button visible as subscribed`() {
        val uiState =
            ConversationViewState(
                loginPromptMessageThreshold = 1,
                subscriptionMandatoryThreshold = 1,
                isSubscriptionEnabled = true,
                isSocialSignedIn = true,
                isInfluencerSubscriptionAvailableToPurchase = false,
                isInfluencerSubscriptionPurchasedAndVerified = true,
            ).headerSubscribeButtonUiState()

        assertTrue(uiState.shouldShow)
        assertTrue(uiState.isSubscribed)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `loading chat access shows loader instead of subscribe`() {
        val uiState =
            ConversationViewState(
                loginPromptMessageThreshold = 1,
                subscriptionMandatoryThreshold = 1,
                isSubscriptionEnabled = true,
                isSocialSignedIn = true,
                isChatAccessLoading = true,
            ).headerSubscribeButtonUiState()

        assertTrue(uiState.shouldShow)
        assertFalse(uiState.isSubscribed)
        assertTrue(uiState.isLoading)
    }

    @Test
    fun `available products show subscribe before purchase`() {
        val uiState =
            ConversationViewState(
                loginPromptMessageThreshold = 1,
                subscriptionMandatoryThreshold = 1,
                isSubscriptionEnabled = true,
                isSocialSignedIn = true,
                isInfluencerSubscriptionAvailableToPurchase = true,
                isInfluencerSubscriptionPurchasedAndVerified = false,
            ).headerSubscribeButtonUiState()

        assertEquals(
            ConversationSubscribeButtonUiState(
                shouldShow = true,
                isSubscribed = false,
                isLoading = false,
            ),
            uiState,
        )
    }

    @Test
    fun `bot accounts still hide the header subscribe button`() {
        val uiState =
            ConversationViewState(
                loginPromptMessageThreshold = 1,
                subscriptionMandatoryThreshold = 1,
                isSubscriptionEnabled = true,
                isSocialSignedIn = true,
                isBotAccount = true,
                isInfluencerSubscriptionAvailableToPurchase = true,
            ).headerSubscribeButtonUiState()

        assertFalse(uiState.shouldShow)
        assertFalse(uiState.isSubscribed)
        assertFalse(uiState.isLoading)
    }
}
