package com.yral.shared.features.coach.domain

import com.yral.shared.features.coach.domain.models.ApplyCoachProposalResult
import com.yral.shared.features.coach.domain.models.CoachMessage
import com.yral.shared.features.coach.domain.models.CoachSession
import com.yral.shared.features.coach.domain.models.SendCoachMessageResult

interface CoachRepository {
    suspend fun createSession(botId: String): CoachSession

    suspend fun sendMessage(
        coachConversationId: String,
        content: String,
    ): SendCoachMessageResult

    suspend fun applyProposal(coachConversationId: String): ApplyCoachProposalResult

    suspend fun listMessages(coachConversationId: String): List<CoachMessage>
}
