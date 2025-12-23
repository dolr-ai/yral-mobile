package com.yral.shared.features.chat.attachments

import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Platform-agnostic attachment abstraction owned by the chat module.
 *
 * Implementations should provide a readable [Source] plus metadata so the shared layer can
 * build multipart form-data requests.
 */
interface ChatAttachment {
    val fileName: String
    val contentType: String
    val size: Long?

    fun openSource(): Source
}

class FilePathChatAttachment(
    val filePath: String,
    override val fileName: String,
    override val contentType: String,
) : ChatAttachment {
    private val path = Path(filePath)

    override val size: Long? = SystemFileSystem.metadataOrNull(path)?.size

    override fun openSource(): Source = SystemFileSystem.source(path).buffered()

    fun deleteCachedFile(): Boolean =
        runCatching {
            SystemFileSystem.delete(path, mustExist = false)
            true
        }.getOrDefault(false)
}

fun deleteChatCachedFile(filePath: String): Boolean =
    runCatching {
        SystemFileSystem.delete(Path(filePath), mustExist = false)
        true
    }.getOrDefault(false)
