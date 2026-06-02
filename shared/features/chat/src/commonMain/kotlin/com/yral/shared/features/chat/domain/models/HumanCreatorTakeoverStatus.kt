package com.yral.shared.features.chat.domain.models

data class HumanCreatorTakeoverStatus(
    val active: Boolean,
    val startedAt: String?,
    val userLastMessageAt: String?,
    val remainingSeconds: Int,
)
