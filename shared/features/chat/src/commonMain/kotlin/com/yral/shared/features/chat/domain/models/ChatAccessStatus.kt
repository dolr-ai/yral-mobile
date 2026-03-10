package com.yral.shared.features.chat.domain.models

data class ChatAccessStatus(
    val hasAccess: Boolean,
    val expiresAtMs: Long? = null,
)
