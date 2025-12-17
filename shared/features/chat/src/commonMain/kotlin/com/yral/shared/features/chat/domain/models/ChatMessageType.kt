package com.yral.shared.features.chat.domain.models

enum class ChatMessageType(
    val apiValue: String,
) {
    TEXT("text"),
    IMAGE("image"),
    MULTIMODAL("multimodal"),
    AUDIO("audio"),
    ;

    companion object {
        fun fromApi(value: String): ChatMessageType =
            when (value.trim().lowercase()) {
                TEXT.apiValue -> TEXT
                IMAGE.apiValue -> IMAGE
                MULTIMODAL.apiValue -> MULTIMODAL
                AUDIO.apiValue -> AUDIO
                // backend returns uppercase sometimes; normalize
                "text" -> TEXT
                "image" -> IMAGE
                "multimodal" -> MULTIMODAL
                "audio" -> AUDIO
                "TEXT".lowercase() -> TEXT
                "IMAGE".lowercase() -> IMAGE
                "MULTIMODAL".lowercase() -> MULTIMODAL
                "AUDIO".lowercase() -> AUDIO
                else -> TEXT
            }
    }
}
