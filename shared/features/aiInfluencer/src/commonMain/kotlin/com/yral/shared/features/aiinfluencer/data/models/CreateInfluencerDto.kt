package com.yral.shared.features.aiinfluencer.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateInfluencerRequestDto(
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    val description: String,
    @SerialName("system_instructions")
    val systemInstructions: String,
    @SerialName("initial_greeting")
    val initialGreeting: String,
    @SerialName("suggested_messages")
    val suggestedMessages: List<String>,
    @SerialName("personality_traits")
    val personalityTraits: Map<String, String>,
    val category: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("is_nsfw")
    val isNsfw: Boolean,
    @SerialName("bot_principal_id")
    val botPrincipalId: String,
    @SerialName("parent_principal_id")
    val parentPrincipalId: String,
)

@Serializable
data class CreateInfluencerResponseDto(
    val id: String,
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val description: String? = null,
    val category: String? = null,
    @SerialName("is_active")
    val isActive: String? = null,
    @SerialName("parent_principal_id")
    val parentPrincipalId: String? = null,
    val source: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("starter_video_prompt")
    val starterVideoPrompt: String? = null,
)
