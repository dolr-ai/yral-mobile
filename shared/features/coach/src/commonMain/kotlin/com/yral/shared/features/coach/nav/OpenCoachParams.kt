package com.yral.shared.features.coach.nav

import kotlinx.serialization.Serializable

@Serializable
data class OpenCoachParams(
    val botId: String,
    val botName: String? = null,
    val avatarUrl: String? = null,
)
