package com.yral.shared.features.chat.ui.conversation

internal enum class ConversationBottomAreaState {
    BotAccountPrompt,
    BotAccountReadOnly,
    InfluencerSubscription,
    SubscriptionUnavailable,
    ChatInput,
}

internal fun resolveConversationBottomAreaState(
    isBotAccount: Boolean,
    isHumanParticipantConversation: Boolean,
    shouldShowInfluencerSubscriptionCard: Boolean,
    shouldBlockChatNoProduct: Boolean,
): ConversationBottomAreaState =
    when {
        isBotAccount && isHumanParticipantConversation -> ConversationBottomAreaState.BotAccountReadOnly
        isBotAccount -> ConversationBottomAreaState.BotAccountPrompt
        shouldShowInfluencerSubscriptionCard -> ConversationBottomAreaState.InfluencerSubscription
        shouldBlockChatNoProduct -> ConversationBottomAreaState.SubscriptionUnavailable
        else -> ConversationBottomAreaState.ChatInput
    }

internal fun resolveConversationProfilePrincipal(
    isBotAccount: Boolean,
    humanParticipantUserId: String?,
    influencerId: String,
): String =
    when {
        isBotAccount && !humanParticipantUserId.isNullOrBlank() -> humanParticipantUserId
        else -> influencerId
    }
