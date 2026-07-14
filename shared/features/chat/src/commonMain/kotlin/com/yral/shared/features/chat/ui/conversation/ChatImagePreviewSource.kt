package com.yral.shared.features.chat.ui.conversation

import com.yral.shared.features.chat.attachments.FilePathChatAttachment

internal sealed interface ChatImagePreviewSource {
    data class Draft(
        val imageAttachment: FilePathChatAttachment,
    ) : ChatImagePreviewSource

    data class Message(
        val imageUrl: String,
        // Sibling images (e.g. the full collage) — when there's more than one,
        // the preview becomes a swipeable pager starting at [imageUrl].
        val galleryUrls: List<String> = emptyList(),
    ) : ChatImagePreviewSource
}
