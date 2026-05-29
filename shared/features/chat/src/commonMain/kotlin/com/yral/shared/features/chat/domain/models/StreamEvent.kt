package com.yral.shared.features.chat.domain.models

sealed class StreamEvent {
    data class Token(
        val text: String,
    ) : StreamEvent()

    data class Done(
        val assistantMessage: ChatMessage,
        val provider: String,
        val blocked: Boolean,
    ) : StreamEvent()

    // Phase 3 leaves this minimal. Phase 6 wires the inline AssistantErrorBubble
    // off this same type so the streaming and non-streaming paths share rendering.
    data class Failed(
        val code: String,
        val message: String,
        val retryable: Boolean,
    ) : StreamEvent()
}
