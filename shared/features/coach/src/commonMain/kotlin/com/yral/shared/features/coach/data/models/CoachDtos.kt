package com.yral.shared.features.coach.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    /**
     * Coach Fix 1 PR-B — when the creator asks for something that
     * conflicts with a platform-wide rule (response_length,
     * language_mirror), Coach emits this field INSTEAD of
     * `proposed_changes`. On /apply, backend merges into
     * `ai_influencers.global_rule_overrides`. Mobile treats override
     * messages as proposals for card-rendering + Apply purposes; the
     * structured payload itself ({key, value}) stays opaque on this
     * surface because the reasoning text already explains the change.
     * Future PR-B mobile UX (per-bullet "tap to override" entry from
     * the Soul File summary) can read the typed contents separately.
     */
    @SerialName("proposed_global_rule_override") val proposedGlobalRuleOverride: JsonElement? = null,
    @SerialName("reasoning") val reasoning: String? = null,
    @SerialName("suggestions") val suggestions: List<String>? = null,
    @SerialName("applied") val applied: Boolean = false,
    /**
     * PR-3 (migration 035) — proposal lifecycle on `coach_messages.status`.
     * One of `pending` / `applied` / `superseded` / `discarded` / `na`.
     * `na` covers non-proposal rows (creator messages, opening, receipt).
     * Defaults to `"na"` for backward-compat with older backends.
     *
     * Mobile drives card state off this: PENDING = active Apply button,
     * APPLIED = "Applied" disabled label, SUPERSEDED = "Replaced by newer
     * proposal" subtitle + Apply disabled.
     */
    @SerialName("status") val status: String = "na",
    @SerialName("status_changed_at") val statusChangedAt: String? = null,
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

/**
 * Coach pivot Bucket 1 Item 3 (PR-3, migration 035) — `/apply` now
 * requires the caller to name the specific proposal being applied.
 * Pre-PR-3 the endpoint implicitly picked "most recent pending,"
 * which silently applied newer proposals when the creator scrolled
 * up and tapped Save on an older card. Sending the explicit id from
 * the card the user actually tapped closes that trust gap.
 *
 * Backend responses to know about:
 *  - 422 — proposal_id missing or blank
 *  - 404 — id not in this session (likely cross-session mismatch)
 *  - 409 — proposal exists but its status isn't 'pending' (e.g. it
 *    was superseded by a newer one or already applied). Body carries
 *    `current_status`.
 */
@Serializable
data class ApplyCoachProposalRequestDto(
    @SerialName("proposal_id") val proposalId: String,
)

@Serializable
data class ApplyCoachProposalResponseDto(
    @SerialName("applied") val applied: Boolean,
    @SerialName("history_id") val historyId: String,
    /**
     * Personality-edit apply path returns these as the full text
     * before/after of `system_instructions`. The override and
     * sectioned-edit apply paths return DIFFERENT fields (override_key/
     * override_value, or section-specific) and OMIT these — so they
     * MUST be nullable. Backend dispatches the apply response shape
     * based on which proposal type ran; mobile only ever reads
     * `receipt_message`, so a missing pair is harmless. Pre-2026-06-12
     * mobile declared these as non-nullable, which caused the
     * `MissingFieldException` Rishi hit on every override apply — the
     * backend had already flipped status='applied' but mobile surfaced
     * "could not apply" because deserialization threw.
     */
    @SerialName("previous_instructions") val previousInstructions: String? = null,
    @SerialName("new_instructions") val newInstructions: String? = null,
    @SerialName("applied_at") val appliedAt: String? = null,
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
