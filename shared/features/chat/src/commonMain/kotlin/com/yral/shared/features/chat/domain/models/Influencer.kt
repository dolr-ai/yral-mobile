package com.yral.shared.features.chat.domain.models

data class Influencer(
    val id: String,
    val name: String,
    val displayName: String,
    val avatarUrl: String,
    val description: String,
    val category: String,
    val isActive: String,
    val createdAt: String,
    val conversationCount: Int?,
)
