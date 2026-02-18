package com.yral.shared.features.aiinfluencer.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateAndGenerateMetadataRequestDto(
    @SerialName("system_instructions")
    val systemInstructions: String,
)
