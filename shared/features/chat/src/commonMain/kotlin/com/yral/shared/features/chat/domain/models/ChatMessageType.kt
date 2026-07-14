package com.yral.shared.features.chat.domain.models

enum class ChatMessageType(
    val apiValue: String,
) {
    TEXT("text"),
    IMAGE("image"),
    MULTIMODAL("multimodal"),
    AUDIO("audio"),
    COLLAGE("collage"),
    ;

    companion object {
        // Lowercasing also normalizes the uppercase variants the backend
        // sometimes returns. Unknown types degrade to TEXT so old payloads
        // (and new server-side types) render the content string, not crash.
        fun fromApi(value: String): ChatMessageType {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.apiValue == normalized } ?: TEXT
        }
    }
}
