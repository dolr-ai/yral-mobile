package com.yral.shared.features.aiinfluencer.data

import com.yral.shared.features.aiinfluencer.data.models.CreateInfluencerRequestDto
import com.yral.shared.features.aiinfluencer.data.models.CreateInfluencerResponseDto
import com.yral.shared.features.aiinfluencer.data.models.GeneratePromptResponseDto
import com.yral.shared.features.aiinfluencer.data.models.ValidateAndGenerateMetadataResponseDto

interface AiInfluencerDataSource {
    suspend fun generatePrompt(prompt: String): GeneratePromptResponseDto

    suspend fun validateAndGenerateMetadata(systemInstructions: String): ValidateAndGenerateMetadataResponseDto

    suspend fun createInfluencer(request: CreateInfluencerRequestDto): CreateInfluencerResponseDto
}
