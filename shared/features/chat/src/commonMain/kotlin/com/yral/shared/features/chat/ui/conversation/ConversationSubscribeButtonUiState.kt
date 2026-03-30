package com.yral.shared.features.chat.ui.conversation

import com.yral.shared.features.chat.viewmodel.ConversationViewState

internal data class ConversationSubscribeButtonUiState(
    val shouldShow: Boolean,
    val isSubscribed: Boolean,
)

internal fun ConversationViewState.headerSubscribeButtonUiState(): ConversationSubscribeButtonUiState =
    ConversationSubscribeButtonUiState(
        shouldShow =
            isSocialSignedIn &&
                isSubscriptionEnabled &&
                !isBotAccount &&
                (
                    isInfluencerSubscriptionPurchasedAndVerified ||
                        isInfluencerSubscriptionAvailableToPurchase
                ),
        isSubscribed = isInfluencerSubscriptionPurchasedAndVerified,
    )
