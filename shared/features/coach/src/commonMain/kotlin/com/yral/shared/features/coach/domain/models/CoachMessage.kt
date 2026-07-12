package com.yral.shared.features.coach.domain.models

data class CoachMessage(
    val id: String,
    val coachConversationId: String,
    val role: CoachMessageRole,
    val content: String,
    val proposedChanges: String? = null,
    /**
     * Coach Fix 1 PR-B — true when this turn proposed a per-bot
     * platform-rule override (response_length, language_mirror) via
     * the backend's `proposed_global_rule_override` field. Mobile
     * treats this the same as a regular `proposedChanges` proposal
     * for proposal-card + Apply rendering; backend dispatches the
     * correct write on /apply based on which field is set.
     */
    val hasOverrideProposal: Boolean = false,
    val reasoning: String? = null,
    val suggestions: List<String>? = null,
    /**
     * Legacy bool from pre-PR-3 days. Now derived from [status] for
     * the existing call-sites that read it; kept until those are
     * migrated. New code should branch on [status] instead.
     */
    val applied: Boolean = false,
    /**
     * PR-3 (migration 035) — proposal lifecycle. Drives the
     * Apply-card-state rendering. Non-proposal rows (creator messages,
     * opening, receipt) carry NA so the UI doesn't have to special-case.
     */
    val status: ProposalStatus = ProposalStatus.NA,
    val statusChangedAt: String? = null,
    /**
     * Mobile-only flag set when this message arrived as the apply
     * response's `receipt_message`. Drives the "Continue coaching /
     * I'm done for now" CTA pair render below the latest receipt.
     * Backend doesn't currently distinguish receipts from other coach
     * rows — confirmed with Session 6 that local tracking is fine.
     */
    val isReceipt: Boolean = false,
    val createdAt: String,
) {
    val hasProposal: Boolean
        get() = !proposedChanges.isNullOrBlank() || hasOverrideProposal

    val hasSuggestions: Boolean
        get() = !suggestions.isNullOrEmpty()
}

/**
 * PR-3 (migration 035) `coach_messages.status` enum, mapped from the
 * backend string. Unknown slugs (forward-compat additions) collapse to
 * NA so we never crash on a new lifecycle state.
 */
enum class ProposalStatus(val apiValue: String) {
    PENDING("pending"),
    APPLIED("applied"),
    SUPERSEDED("superseded"),
    DISCARDED("discarded"),
    NA("na"),
    ;

    companion object {
        fun fromApi(value: String?): ProposalStatus =
            entries.firstOrNull { it.apiValue == value } ?: NA
    }
}
