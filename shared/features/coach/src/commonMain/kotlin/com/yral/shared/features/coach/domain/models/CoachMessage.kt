package com.yral.shared.features.coach.domain.models

data class CoachMessage(
    val id: String,
    val coachConversationId: String,
    val role: CoachMessageRole,
    val content: String,
    val proposedChanges: String? = null,
    val reasoning: String? = null,
    val applied: Boolean = false,
    val createdAt: String,
) {
    val hasProposal: Boolean
        get() = !proposedChanges.isNullOrBlank()
}
