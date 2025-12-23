package com.yral.shared.features.chat.attachments

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

/**
 * Persists a picked/captured [Uri] into app cache and returns a [FilePathChatAttachment].
 *
 * This avoids relying on `content://` URIs during upload/retry and keeps shared logic file-based.
 * Intended for small images + short audio.
 */
fun persistUriToChatCache(
    context: Context,
    uri: Uri,
    contentTypeOverride: String? = null,
    fileNameOverride: String? = null,
): FilePathChatAttachment {
    val cacheDir = File(context.cacheDir, CHAT_ATTACHMENTS_DIR).apply { mkdirs() }

    val contentType = contentTypeOverride ?: context.contentResolver.getType(uri) ?: DEFAULT_CONTENT_TYPE
    val displayName = fileNameOverride ?: queryDisplayName(context, uri)
    val extension = guessExtension(displayName, contentType)

    val safeBaseName =
        (displayName?.substringBeforeLast('.') ?: "attachment_${System.currentTimeMillis()}")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
    val uniqueSuffix = "_${System.currentTimeMillis()}"
    val fileName =
        if (extension.isNullOrBlank()) {
            "$safeBaseName$uniqueSuffix"
        } else {
            "$safeBaseName$uniqueSuffix.$extension"
        }

    val destFile = File(cacheDir, fileName)

    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open input stream for $uri" }
        FileOutputStream(destFile).use { output ->
            input.copyTo(output)
        }
    }

    return FilePathChatAttachment(
        filePath = destFile.absolutePath,
        fileName = destFile.name,
        contentType = contentType,
    )
}

private fun queryDisplayName(
    context: Context,
    uri: Uri,
): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        cursor.getStringOrNull(OpenableColumns.DISPLAY_NAME)
    }

private fun Cursor.getStringOrNull(columnName: String): String? {
    val idx = getColumnIndex(columnName)
    return if (idx >= 0 && !isNull(idx)) getString(idx) else null
}

private fun guessExtension(
    displayName: String?,
    contentType: String,
): String? {
    // Prefer existing extension if present
    val existing = displayName?.substringAfterLast('.', missingDelimiterValue = "")?.takeIf { it.isNotBlank() }
    if (!existing.isNullOrBlank()) return existing

    return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
}

private const val CHAT_ATTACHMENTS_DIR = "chat_attachments"
private const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
