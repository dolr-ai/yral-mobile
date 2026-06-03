package com.yral.shared.features.coach.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoachSessionDto(
    @SerialName("id") val id: String,
    @SerialName("bot_id") val botId: String,
    @SerialName("bot_name") val botName: String? = null,
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
    @SerialName("applied") val applied: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class SendCoachMessageRequestDto(
    @SerialName("content") val content: String,
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
)

@Serializable
data class ListCoachMessagesResponseDto(
    @SerialName("coach_conversation_id") val coachConversationId: String,
    @SerialName("messages") val messages: List<CoachMessageDto>,
    @SerialName("total") val total: Int,
)
