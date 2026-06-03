package com.yral.shared.features.coach.domain.models

data class CoachSession(
    val id: String,
    val botId: String,
    val botName: String?,
    val createdAt: String,
)

data class SendCoachMessageResult(
    val creatorMessage: CoachMessage,
    val coachMessage: CoachMessage,
)

data class ApplyCoachProposalResult(
    val applied: Boolean,
    val historyId: String,
    val previousInstructions: String,
    val newInstructions: String,
    val appliedAt: String,
)
