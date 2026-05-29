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
