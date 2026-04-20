package com.yral.shared.features.chat.attachments

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatImageAttachmentMetadataTest {
    @Test
    fun `build picked chat image metadata uses jpeg extension and content type`() {
        assertEquals(
            ChatImageAttachmentMetadata(
                fileName = "chat_image_123.jpg",
                contentType = "image/jpeg",
            ),
            buildPickedChatImageMetadata(
                timestampMs = 123,
                format = ChatPickedImageFormat.JPEG,
            ),
        )
    }

    @Test
    fun `build picked chat image metadata uses png extension and content type`() {
        assertEquals(
            ChatImageAttachmentMetadata(
                fileName = "chat_image_456.png",
                contentType = "image/png",
            ),
            buildPickedChatImageMetadata(
                timestampMs = 456,
                format = ChatPickedImageFormat.PNG,
            ),
        )
    }

    @Test
    fun `infer chat attachment content type prefers explicit override`() {
        assertEquals(
            "image/webp",
            inferChatAttachmentContentType(
                fileName = "chat_image.jpg",
                contentTypeOverride = "image/webp",
            ),
        )
    }

    @Test
    fun `infer chat attachment content type falls back to filename extension`() {
        assertEquals(
            "image/jpeg",
            inferChatAttachmentContentType(
                fileName = "chat_image.jpeg",
                contentTypeOverride = null,
            ),
        )
        assertEquals(
            "image/png",
            inferChatAttachmentContentType(
                fileName = "chat_image.png",
                contentTypeOverride = null,
            ),
        )
    }

    @Test
    fun `infer chat attachment content type falls back to default for unknown extension`() {
        assertEquals(
            DEFAULT_CHAT_ATTACHMENT_CONTENT_TYPE,
            inferChatAttachmentContentType(
                fileName = "chat_image.bin",
                contentTypeOverride = null,
            ),
        )
    }
}
