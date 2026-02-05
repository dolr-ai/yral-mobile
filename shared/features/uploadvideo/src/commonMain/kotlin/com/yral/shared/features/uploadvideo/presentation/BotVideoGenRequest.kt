package com.yral.shared.features.uploadvideo.presentation

import kotlinx.serialization.Serializable

@Serializable
data class BotVideoGenRequest(
    val principal: String,
    val counter: Long,
    val modelName: String,
    val prompt: String,
)
