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

    // Phase 4 makes this typed. Phase 6 wires the inline AssistantErrorBubble
    // off this same [AssistantError] so the streaming and non-streaming paths
    // share rendering.
    data class Failed(
        val error: AssistantError,
    ) : StreamEvent()
}
