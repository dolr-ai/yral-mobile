package com.yral.shared.features.coach.data.models

import com.yral.shared.features.coach.domain.models.ApplyCoachProposalResult
import com.yral.shared.features.coach.domain.models.CoachMessage
import com.yral.shared.features.coach.domain.models.CoachMessageRole
import com.yral.shared.features.coach.domain.models.CoachSession
import com.yral.shared.features.coach.domain.models.SendCoachMessageResult

fun CoachSessionDto.toDomain(): CoachSession =
    CoachSession(
        id = id,
        botId = botId,
        botName = botName,
        resumed = resumed,
        createdAt = createdAt,
    )

fun CoachMessageDto.toDomain(): CoachMessage =
    CoachMessage(
        id = id,
        coachConversationId = coachConversationId,
        role = CoachMessageRole.fromApi(role),
        content = content,
        proposedChanges = proposedChanges,
        reasoning = reasoning,
        suggestions = suggestions,
        applied = applied,
        createdAt = createdAt,
    )

fun SendCoachMessageResponseDto.toDomain(): SendCoachMessageResult =
    SendCoachMessageResult(
        creatorMessage = creatorMessage.toDomain(),
        coachMessage = coachMessage.toDomain(),
    )

fun ApplyCoachProposalResponseDto.toDomain(): ApplyCoachProposalResult =
    ApplyCoachProposalResult(
        applied = applied,
        historyId = historyId,
        previousInstructions = previousInstructions,
        newInstructions = newInstructions,
        appliedAt = appliedAt,
        receiptMessage = receiptMessage?.toDomain(),
    )

fun ListCoachMessagesResponseDto.toDomain(): List<CoachMessage> = messages.map { it.toDomain() }
