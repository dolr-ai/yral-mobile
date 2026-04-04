package com.yral.shared.features.chat.ui.conversation

internal enum class ConversationBottomAreaState {
    BotAccountPrompt,
    InfluencerSubscription,
    SubscriptionUnavailable,
    ChatInput,
}

internal fun resolveConversationBottomAreaState(
    isBotAccount: Boolean,
    shouldShowInfluencerSubscriptionCard: Boolean,
    shouldBlockChatNoProduct: Boolean,
): ConversationBottomAreaState =
    when {
        isBotAccount -> ConversationBottomAreaState.BotAccountPrompt
        shouldShowInfluencerSubscriptionCard -> ConversationBottomAreaState.InfluencerSubscription
        shouldBlockChatNoProduct -> ConversationBottomAreaState.SubscriptionUnavailable
        else -> ConversationBottomAreaState.ChatInput
    }
