package com.yral.shared.features.aiinfluencer.data

import com.yral.shared.features.aiinfluencer.data.models.toDomain
import com.yral.shared.features.aiinfluencer.domain.AiInfluencerRepository
import com.yral.shared.features.aiinfluencer.domain.models.CreatedInfluencer
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedPrompt

class AiInfluencerRepositoryImpl(
    private val dataSource: AiInfluencerDataSource,
) : AiInfluencerRepository {
    override suspend fun generatePrompt(prompt: String): GeneratedPrompt = dataSource.generatePrompt(prompt).toDomain()

    override suspend fun validateAndGenerateMetadata(systemInstructions: String): GeneratedInfluencerMetadata =
        dataSource.validateAndGenerateMetadata(systemInstructions).toDomain()

    override suspend fun createInfluencer(request: CreatedInfluencer): CreatedInfluencer =
        dataSource
            .createInfluencer(
                com.yral.shared.features.aiinfluencer.data.models.CreateInfluencerRequestDto(
                    name = request.name,
                    displayName = request.displayName,
                    description = request.description,
                    systemInstructions = request.systemInstructions,
                    initialGreeting = request.initialGreeting,
                    suggestedMessages = request.suggestedMessages,
                    personalityTraits = request.personalityTraits,
                    category = request.category,
                    avatarUrl = request.avatarUrl,
                    isNsfw = request.isNsfw,
                    botPrincipalId = request.botPrincipalId,
                    parentPrincipalId = request.parentPrincipalId,
                ),
            ).let {
                request.copy(
                    avatarUrl = it.avatarUrl ?: request.avatarUrl,
                    starterVideoPrompt = it.starterVideoPrompt,
                )
            }
}
