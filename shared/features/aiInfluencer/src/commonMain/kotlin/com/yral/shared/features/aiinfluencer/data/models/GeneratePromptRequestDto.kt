package com.yral.shared.features.aiinfluencer.data.models

import kotlinx.serialization.Serializable

@Serializable
data class GeneratePromptRequestDto(
    val prompt: String,
)
