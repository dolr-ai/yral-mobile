package com.yral.shared.features.aiinfluencer.data

import com.yral.shared.features.aiinfluencer.data.models.toDomain
import com.yral.shared.features.aiinfluencer.domain.AiInfluencerRepository
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedPrompt

class AiInfluencerRepositoryImpl(
    private val dataSource: AiInfluencerDataSource,
) : AiInfluencerRepository {
    override suspend fun generatePrompt(prompt: String): GeneratedPrompt = dataSource.generatePrompt(prompt).toDomain()

    override suspend fun validateAndGenerateMetadata(systemInstructions: String): GeneratedInfluencerMetadata =
        dataSource.validateAndGenerateMetadata(systemInstructions).toDomain()
}
