package com.yral.shared.features.aiinfluencer.data.models

import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedPrompt

fun GeneratePromptResponseDto.toDomain(): GeneratedPrompt = GeneratedPrompt(systemInstructions = systemInstructions)

fun ValidateAndGenerateMetadataResponseDto.toDomain(): GeneratedInfluencerMetadata =
    GeneratedInfluencerMetadata(
        isValid = isValid,
        reason = reason.orEmpty(),
        name = name.orEmpty(),
        displayName = displayName.orEmpty(),
        description = description.orEmpty(),
        initialGreeting = initialGreeting.orEmpty(),
        suggestedMessages = suggestedMessages ?: emptyList(),
        personalityTraits = personalityTraits ?: emptyMap(),
        category = category.orEmpty(),
        avatarUrl = avatarUrl.orEmpty(),
        systemInstructions = systemInstructions,
        isNsfw = isNsfw,
    )
