package com.yral.shared.features.chat.domain.models

data class ConversationsPageResult(
    val conversations: List<Conversation>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val nextOffset: Int?,
    val rawCount: Int,
)
