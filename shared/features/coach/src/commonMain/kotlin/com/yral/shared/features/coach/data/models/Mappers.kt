package com.yral.shared.features.coach.data.models

import com.yral.shared.features.coach.domain.models.ApplyCoachProposalResult
import com.yral.shared.features.coach.domain.models.CoachMessage
import com.yral.shared.features.coach.domain.models.CoachMessageRole
import com.yral.shared.features.coach.domain.models.CoachMessagesPage
import com.yral.shared.features.coach.domain.models.CoachSession
import com.yral.shared.features.coach.domain.models.ProposalStatus
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
        // Override-style proposals come on a different field than
        // proposed_changes (Coach Fix 1 PR-B). Mobile treats both as
        // "this turn is a proposal" for card-rendering; backend
        // dispatches the correct write on /apply.
        hasOverrideProposal = proposedGlobalRuleOverride != null,
        reasoning = reasoning,
        suggestions = suggestions,
        applied = applied,
        status = ProposalStatus.fromApi(status),
        statusChangedAt = statusChangedAt,
        // `isReceipt` is set in CoachViewModel ONLY when the message
        // arrived via the `apply` response's `receipt_message` field.
        // Everything coming through this mapper (sendMessage, list) is
        // not-receipt by default.
        isReceipt = false,
        createdAt = createdAt,
    )

fun SendCoachMessageResponseDto.toDomain(): SendCoachMessageResult =
    SendCoachMessageResult(
        creatorMessage = creatorMessage.toDomain(),
        coachMessage = coachMessage.toDomain(),
        pendingProposalExists = pendingProposalExists,
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

fun ListCoachMessagesResponseDto.toDomain(): CoachMessagesPage =
    CoachMessagesPage(
        messages = messages.map { it.toDomain() },
        pendingProposalExists = pendingProposalExists,
    )
