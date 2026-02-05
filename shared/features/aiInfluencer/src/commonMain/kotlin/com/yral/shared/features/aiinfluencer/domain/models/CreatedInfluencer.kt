package com.yral.shared.features.aiinfluencer.domain.models

data class CreatedInfluencer(
    val name: String,
    val displayName: String,
    val description: String,
    val systemInstructions: String,
    val initialGreeting: String,
    val suggestedMessages: List<String>,
    val personalityTraits: Map<String, String>,
    val category: String,
    val avatarUrl: String,
    val isNsfw: Boolean,
    val botPrincipalId: String,
    val parentPrincipalId: String,
    val starterVideoPrompt: String? = null,
)
