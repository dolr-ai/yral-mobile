package com.yral.shared.features.chat.attachments

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToURL

/**
 * Persists a picked/captured [NSURL] into app cache and returns a [FilePathChatAttachment].
 *
 * Intended for small images + short audio.
 */
@OptIn(ExperimentalForeignApi::class)
fun persistUrlToChatCache(
    url: NSURL,
    contentTypeOverride: String? = null,
    fileNameOverride: String? = null,
): FilePathChatAttachment {
    val cachesArray =
        NSFileManager.defaultManager.URLsForDirectory(
            directory = NSCachesDirectory,
            inDomains = NSUserDomainMask,
        )
    val cachesUrl =
        (cachesArray.first() as? NSURL)
            ?: error("Unable to resolve caches directory URL")

    val dirUrl = cachesUrl.URLByAppendingPathComponent(CHAT_ATTACHMENTS_DIR)!!
    NSFileManager.defaultManager.createDirectoryAtURL(
        url = dirUrl,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )

    val fileName = fileNameOverride ?: (url.lastPathComponent ?: "attachment_${currentTimeMs()}")
    val destUrl = dirUrl.URLByAppendingPathComponent(fileName)!!

    val data = requireNotNull(NSData.dataWithContentsOfURL(url)) { "Unable to read data from URL: $url" }
    require(data.writeToURL(destUrl, atomically = true)) { "Unable to write data to cache: $destUrl" }

    val contentType = contentTypeOverride ?: DEFAULT_CONTENT_TYPE
    val destPath = requireNotNull(destUrl.path) { "Destination path is null: $destUrl" }
    return FilePathChatAttachment(
        filePath = destPath,
        fileName = fileName,
        contentType = contentType,
    )
}

private fun currentTimeMs(): Long =
    platform.Foundation
        .NSDate()
        .timeIntervalSince1970
        .toLong() * SECONDS_TO_MILLIS_FACTOR

private const val CHAT_ATTACHMENTS_DIR = "chat_attachments"
private const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
private const val SECONDS_TO_MILLIS_FACTOR = 1000L
