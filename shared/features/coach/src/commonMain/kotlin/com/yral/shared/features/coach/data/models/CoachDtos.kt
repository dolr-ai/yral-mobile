package com.yral.shared.features.coach.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateCoachSessionRequestDto(
    @SerialName("fresh") val fresh: Boolean = false,
)

@Serializable
data class CoachSessionDto(
    @SerialName("id") val id: String,
    @SerialName("bot_id") val botId: String,
    @SerialName("bot_name") val botName: String? = null,
    @SerialName("resumed") val resumed: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class CoachMessageDto(
    @SerialName("id") val id: String,
    @SerialName("coach_conversation_id") val coachConversationId: String,
    @SerialName("role") val role: String,
    @SerialName("content") val content: String,
    @SerialName("proposed_changes") val proposedChanges: String? = null,
    @SerialName("reasoning") val reasoning: String? = null,
    @SerialName("suggestions") val suggestions: List<String>? = null,
    @SerialName("applied") val applied: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class SendCoachMessageRequestDto(
    @SerialName("content") val content: String,
    @SerialName("request_proposal") val requestProposal: Boolean = false,
)

@Serializable
data class SendCoachMessageResponseDto(
    @SerialName("creator_message") val creatorMessage: CoachMessageDto,
    @SerialName("coach_message") val coachMessage: CoachMessageDto,
    // PR-4 (2026-06-11) — mobile gates the Save button on this. Backend
    // computes against post-turn state so a proposal emitted this turn
    // is reflected immediately. Defaulting to `false` keeps older
    // backends backward-compatible.
    @SerialName("pending_proposal_exists") val pendingProposalExists: Boolean = false,
)

@Serializable
data class ApplyCoachProposalResponseDto(
    @SerialName("applied") val applied: Boolean,
    @SerialName("history_id") val historyId: String,
    @SerialName("previous_instructions") val previousInstructions: String,
    @SerialName("new_instructions") val newInstructions: String,
    @SerialName("applied_at") val appliedAt: String,
    @SerialName("receipt_message") val receiptMessage: CoachMessageDto? = null,
)

@Serializable
data class ListCoachMessagesResponseDto(
    @SerialName("coach_conversation_id") val coachConversationId: String,
    @SerialName("messages") val messages: List<CoachMessageDto>,
    @SerialName("total") val total: Int,
    // PR-4 (2026-06-11) — present on session reload so the Save button
    // shows the correct state without scanning every message for an
    // unapplied proposal. Defaults to `false` for older backends.
    @SerialName("pending_proposal_exists") val pendingProposalExists: Boolean = false,
)
