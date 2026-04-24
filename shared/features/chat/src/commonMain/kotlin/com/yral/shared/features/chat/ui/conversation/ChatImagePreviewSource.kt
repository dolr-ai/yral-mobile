package com.yral.shared.features.chat.ui.conversation

import com.yral.shared.features.chat.attachments.FilePathChatAttachment

internal sealed interface ChatImagePreviewSource {
    data class Draft(
        val imageAttachment: FilePathChatAttachment,
    ) : ChatImagePreviewSource

    data class Message(
        val imageUrl: String,
    ) : ChatImagePreviewSource
}
