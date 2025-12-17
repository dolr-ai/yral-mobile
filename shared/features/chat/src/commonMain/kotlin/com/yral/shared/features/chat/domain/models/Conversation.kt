package com.yral.shared.features.chat.domain.models

data class Conversation(
    val id: String,
    val userId: String,
    val influencer: ConversationInfluencer,
    val createdAt: String,
    val updatedAt: String,
    val messageCount: Int,
)

data class ConversationInfluencer(
    val id: String,
    val name: String,
    val displayName: String,
    val avatarUrl: String,
)
