package com.yral.shared.features.chat.attachments

internal data class ChatImageAttachmentMetadata(
    val fileName: String,
    val contentType: String,
)

internal enum class ChatPickedImageFormat(
    val fileExtension: String,
    val contentType: String,
) {
    JPEG(
        fileExtension = "jpg",
        contentType = "image/jpeg",
    ),
    PNG(
        fileExtension = "png",
        contentType = "image/png",
    ),
}

internal fun buildPickedChatImageMetadata(
    timestampMs: Long,
    format: ChatPickedImageFormat,
): ChatImageAttachmentMetadata =
    ChatImageAttachmentMetadata(
        fileName = "chat_image_$timestampMs.${format.fileExtension}",
        contentType = format.contentType,
    )

internal fun inferChatAttachmentContentType(
    fileName: String?,
    contentTypeOverride: String?,
): String =
    contentTypeOverride ?: when (fileName?.substringAfterLast('.', "")?.lowercase()) {
        "jpg",
        "jpeg",
        -> JPEG_CONTENT_TYPE

        "png" -> PNG_CONTENT_TYPE

        else -> DEFAULT_CHAT_ATTACHMENT_CONTENT_TYPE
    }

internal const val DEFAULT_CHAT_ATTACHMENT_CONTENT_TYPE = "application/octet-stream"

private const val JPEG_CONTENT_TYPE = "image/jpeg"
private const val PNG_CONTENT_TYPE = "image/png"
