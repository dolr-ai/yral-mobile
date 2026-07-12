package com.yral.shared.features.coach.data

import com.yral.shared.features.coach.data.models.ApplyCoachProposalRequestDto
import com.yral.shared.features.coach.data.models.CreateCoachSessionRequestDto
import com.yral.shared.features.coach.data.models.SendCoachMessageRequestDto
import com.yral.shared.features.coach.data.models.toDomain
import com.yral.shared.features.coach.domain.CoachRepository
import com.yral.shared.features.coach.domain.models.ApplyCoachProposalResult
import com.yral.shared.features.coach.domain.models.CoachMessagesPage
import com.yral.shared.features.coach.domain.models.CoachSession
import com.yral.shared.features.coach.domain.models.SendCoachMessageResult

class CoachRepositoryImpl(
    private val dataSource: CoachDataSource,
) : CoachRepository {
    override suspend fun createSession(
        botId: String,
        fresh: Boolean,
        sectionHint: String?,
    ): CoachSession =
        dataSource
            .createSession(
                botId = botId,
                request = CreateCoachSessionRequestDto(fresh = fresh, sectionHint = sectionHint),
            ).toDomain()

    override suspend fun sendMessage(
        coachConversationId: String,
        content: String,
        requestProposal: Boolean,
    ): SendCoachMessageResult =
        dataSource
            .sendMessage(
                coachConversationId = coachConversationId,
                request =
                    SendCoachMessageRequestDto(
                        content = content,
                        requestProposal = requestProposal,
                    ),
            ).toDomain()

    override suspend fun applyProposal(
        coachConversationId: String,
        proposalId: String,
    ): ApplyCoachProposalResult =
        dataSource
            .applyProposal(
                coachConversationId = coachConversationId,
                request = ApplyCoachProposalRequestDto(proposalId = proposalId),
            ).toDomain()

    override suspend fun listMessages(coachConversationId: String): CoachMessagesPage =
        dataSource.listMessages(coachConversationId).toDomain()
}
