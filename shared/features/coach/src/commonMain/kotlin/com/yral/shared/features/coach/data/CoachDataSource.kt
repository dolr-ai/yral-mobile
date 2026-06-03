package com.yral.shared.features.coach.data

import com.yral.shared.features.coach.data.models.ApplyCoachProposalResponseDto
import com.yral.shared.features.coach.data.models.CoachSessionDto
import com.yral.shared.features.coach.data.models.ListCoachMessagesResponseDto
import com.yral.shared.features.coach.data.models.SendCoachMessageRequestDto
import com.yral.shared.features.coach.data.models.SendCoachMessageResponseDto

interface CoachDataSource {
    suspend fun createSession(botId: String): CoachSessionDto

    suspend fun sendMessage(
        coachConversationId: String,
        request: SendCoachMessageRequestDto,
    ): SendCoachMessageResponseDto

    suspend fun applyProposal(coachConversationId: String): ApplyCoachProposalResponseDto

    suspend fun listMessages(coachConversationId: String): ListCoachMessagesResponseDto
}
