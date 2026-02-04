package com.yral.shared.features.aiinfluencer.data.models

import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedPrompt

fun GeneratePromptResponseDto.toDomain(): GeneratedPrompt = GeneratedPrompt(systemInstructions = systemInstructions)

fun ValidateAndGenerateMetadataResponseDto.toDomain(): GeneratedInfluencerMetadata =
    GeneratedInfluencerMetadata(
        isValid = isValid,
        reason = reason.orEmpty(),
        name = name,
        displayName = displayName,
        description = description,
        initialGreeting = initialGreeting,
        suggestedMessages = suggestedMessages,
        personalityTraits = personalityTraits,
        category = category,
        avatarUrl = avatarUrl,
        systemInstructions = systemInstructions,
        isNsfw = isNsfw,
    )
