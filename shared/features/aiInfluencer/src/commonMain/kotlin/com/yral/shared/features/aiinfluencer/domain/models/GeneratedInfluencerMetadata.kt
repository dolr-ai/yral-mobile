package com.yral.shared.features.aiinfluencer.domain.models

data class GeneratedInfluencerMetadata(
    val isValid: Boolean,
    val reason: String,
    val name: String,
    val displayName: String,
    val description: String,
    val initialGreeting: String,
    val suggestedMessages: List<String>,
    val personalityTraits: Map<String, String>,
    val category: String,
    val avatarUrl: String,
    val systemInstructions: String? = null,
    val isNsfw: Boolean = false,
)
