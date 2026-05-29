package com.yral.shared.features.chat.domain.models

/**
 * Domain representation of an assistant-side failure. The same type is emitted
 * by the SSE `error` event (Phase 4) and produced from the non-streaming
 * response/exception path (Phase 6), so a single UI renderer
 * (`AssistantErrorBubble`) drives both code paths.
 *
 * `code` is parsed to [AssistantErrorCode] for routing/telemetry; the raw
 * `rawCode` string is preserved verbatim so unknown codes from the backend
 * survive a roundtrip without being silently downgraded.
 */
data class AssistantError(
    val code: AssistantErrorCode,
    val rawCode: String,
    val message: String,
    val retryable: Boolean,
)

/**
 * UI-facing pairing of an [AssistantError] with the draft that produced it.
 * Held by the ViewModel so the retry affordance has the draft on hand
 * without re-deriving it from the user's overlay state.
 *
 * `retryDraft` is null when retry isn't applicable (e.g. error happened on
 * a session-resume re-render, not from a draft we still own).
 */
data class AssistantErrorPresentation(
    val error: AssistantError,
    val retryDraft: SendMessageDraft?,
)

enum class AssistantErrorCode {
    BLOCKED_CONTENT,
    TRANSIENT,
    NO_PROVIDER,
    UNKNOWN,
    ;

    companion object {
        fun fromRaw(raw: String): AssistantErrorCode =
            when (raw) {
                "BLOCKED_CONTENT" -> BLOCKED_CONTENT
                "TRANSIENT" -> TRANSIENT
                "NO_PROVIDER" -> NO_PROVIDER
                else -> UNKNOWN
            }
    }
}
