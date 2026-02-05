package com.yral.shared.features.aiinfluencer.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateAndGenerateMetadataResponseDto(
    @SerialName("is_valid")
    val isValid: Boolean,
    val reason: String? = null,
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    val description: String,
    @SerialName("initial_greeting")
    val initialGreeting: String,
    @SerialName("suggested_messages")
    val suggestedMessages: List<String> = emptyList(),
    @SerialName("personality_traits")
    val personalityTraits: Map<String, String> = emptyMap(),
    val category: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("system_instructions")
    val systemInstructions: String? = null,
    @SerialName("is_nsfw")
    val isNsfw: Boolean = false,
)
