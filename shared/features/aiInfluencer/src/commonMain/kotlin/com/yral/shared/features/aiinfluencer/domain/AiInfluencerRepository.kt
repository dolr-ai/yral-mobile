package com.yral.shared.features.aiinfluencer.domain

import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedPrompt

interface AiInfluencerRepository {
    suspend fun generatePrompt(prompt: String): GeneratedPrompt

    suspend fun validateAndGenerateMetadata(systemInstructions: String): GeneratedInfluencerMetadata
}
