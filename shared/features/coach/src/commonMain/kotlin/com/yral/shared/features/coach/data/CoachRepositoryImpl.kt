package com.yral.shared.features.coach.data

import com.yral.shared.features.coach.data.models.SendCoachMessageRequestDto
import com.yral.shared.features.coach.data.models.toDomain
import com.yral.shared.features.coach.domain.CoachRepository
import com.yral.shared.features.coach.domain.models.ApplyCoachProposalResult
import com.yral.shared.features.coach.domain.models.CoachMessage
import com.yral.shared.features.coach.domain.models.CoachSession
import com.yral.shared.features.coach.domain.models.SendCoachMessageResult

class CoachRepositoryImpl(
    private val dataSource: CoachDataSource,
) : CoachRepository {
    override suspend fun createSession(botId: String): CoachSession = dataSource.createSession(botId).toDomain()

    override suspend fun sendMessage(
        coachConversationId: String,
        content: String,
    ): SendCoachMessageResult =
        dataSource
            .sendMessage(
                coachConversationId = coachConversationId,
                request = SendCoachMessageRequestDto(content = content),
            ).toDomain()

    override suspend fun applyProposal(coachConversationId: String): ApplyCoachProposalResult =
        dataSource.applyProposal(coachConversationId).toDomain()

    override suspend fun listMessages(coachConversationId: String): List<CoachMessage> =
        dataSource.listMessages(coachConversationId).toDomain()
}
