package com.yral.shared.features.coach.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateCoachSessionRequestDto(
    @SerialName("fresh") val fresh: Boolean = false,
    /**
     * Coach pivot Bucket 2 — when the creator opened Coach by tapping
     * a specific section card on the Soul File page, mobile passes
     * the section's stable `id` slug here. Backend uses it to default
     * Coach's proposals to that section. Optional — omitted when the
     * creator opens Coach from the generic "Make your AI Influencer
     * better" button.
     */
    @SerialName("section_hint") val sectionHint: String? = null,
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
)
