package com.yral.shared.features.coach.domain.models

data class CoachSession(
    val id: String,
    val botId: String,
    val botName: String?,
    val resumed: Boolean,
    val createdAt: String,
)

data class SendCoachMessageResult(
    val creatorMessage: CoachMessage,
    val coachMessage: CoachMessage,
    /**
     * PR-4 (2026-06-11) — does an unapplied proposal exist in the
     * conversation right after this send? Used by the Save button gate.
     */
    val pendingProposalExists: Boolean = false,
)

/**
 * PR-4 (2026-06-11) — list-messages endpoint now returns the
 * `pending_proposal_exists` flag alongside the message list. Wrapped
 * here so the screen can drive the Save button state from a single
 * domain object rather than threading the bool separately.
 */
data class CoachMessagesPage(
    val messages: List<CoachMessage>,
    val pendingProposalExists: Boolean,
)

data class ApplyCoachProposalResult(
    val applied: Boolean,
    val historyId: String,
    val previousInstructions: String,
    val newInstructions: String,
    val appliedAt: String,
    val receiptMessage: CoachMessage? = null,
)
