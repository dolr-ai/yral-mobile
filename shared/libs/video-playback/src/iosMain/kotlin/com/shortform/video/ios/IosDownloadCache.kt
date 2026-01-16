package com.shortform.video.ios

import com.shortform.video.MediaDescriptor
import com.shortform.video.cacheKey
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSUserDomainMask
import platform.Foundation.downloadTaskWithRequest
import platform.Foundation.downloadTaskWithURL
import platform.Foundation.setValue
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
internal class IosDownloadCache(
    private val maxBytes: Long,
) {
    private val fileManager = NSFileManager.defaultManager
    private val cacheDir: NSURL = ensureCacheDir()
    private val session = NSURLSession.sessionWithConfiguration(
        NSURLSessionConfiguration.defaultSessionConfiguration,
    )
    private val activeDownloads = mutableMapOf<String, NSURLSessionDownloadTask>()

    fun cachedFileUrl(descriptor: MediaDescriptor): NSURL? {
        val key = descriptor.cacheKey()
        val url = cacheDir.URLByAppendingPathComponent("$key.mp4")
        return if (url?.path != null && fileManager.fileExistsAtPath(url.path ?: "")) {
            touch(url)
            url
        } else {
            null
        }
    }

    fun prefetch(
        descriptor: MediaDescriptor,
        onComplete: (bytes: Long, fromCache: Boolean) -> Unit,
        onError: () -> Unit,
    ) {
        val key = descriptor.cacheKey()
        val existing = cachedFileUrl(descriptor)
        if (existing != null) {
            onComplete(fileSize(existing), true)
            return
        }
        if (activeDownloads.containsKey(key)) return

        val remoteUrl = NSURL.URLWithString(descriptor.uri) ?: run {
            onError()
            return
        }
        val request = if (descriptor.headers.isNotEmpty()) {
            NSMutableURLRequest(uRL = remoteUrl).apply {
                descriptor.headers.forEach { (headerKey, headerValue) ->
                    setValue(headerValue, forHTTPHeaderField = headerKey)
                }
            }
        } else {
            null
        }
        val task = if (request != null) {
            session.downloadTaskWithRequest(request) { tempUrl, _, error ->
                handleDownloadResult(tempUrl, error, key, onComplete, onError)
            }
        } else {
            session.downloadTaskWithURL(remoteUrl) { tempUrl, _, error ->
                handleDownloadResult(tempUrl, error, key, onComplete, onError)
            }
        }
        activeDownloads[key] = task
        task.resume()
    }

    private fun handleDownloadResult(
        tempUrl: NSURL?,
        error: platform.Foundation.NSError?,
        key: String,
        onComplete: (bytes: Long, fromCache: Boolean) -> Unit,
        onError: () -> Unit,
    ) {
        if (error != null || tempUrl == null) {
            activeDownloads.remove(key)
            onError()
            return
        }
        val destination = cacheDir.URLByAppendingPathComponent("$key.mp4") ?: return
        moveToCache(tempUrl, destination)
        val bytes = fileSize(destination)
        activeDownloads.remove(key)
        trimToSize()
        onComplete(bytes, false)
    }

    fun cancelPrefetch(descriptor: MediaDescriptor) {
        val key = descriptor.cacheKey()
        activeDownloads.remove(key)?.cancel()
    }

    fun cancelAll() {
        val keys = activeDownloads.keys.toList()
        for (key in keys) {
            activeDownloads.remove(key)?.cancel()
        }
    }

    private fun ensureCacheDir(): NSURL {
        val urls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
        val base = urls.firstOrNull() as? NSURL
            ?: NSURL.fileURLWithPath(NSTemporaryDirectory())
        val dir = base.URLByAppendingPathComponent("video-playback-cache")!!
        fileManager.createDirectoryAtURL(
            dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return dir
    }

    private fun moveToCache(tempUrl: NSURL, destination: NSURL) {
        tryRemove(destination)
        fileManager.moveItemAtURL(tempUrl, destination, null)
    }

    private fun fileSize(url: NSURL): Long {
        val path = url.path ?: return 0L
        val attributes = fileManager.attributesOfItemAtPath(path, null)
        val sizeNumber = attributes?.get(NSFileSize) as? NSNumber
        return sizeNumber?.longLongValue ?: 0L
    }

    private fun touch(url: NSURL) {
        val path = url.path ?: return
        val attributes: Map<Any?, *> = mapOf(NSFileModificationDate to NSDate())
        fileManager.setAttributes(attributes, ofItemAtPath = path, error = null)
    }

    private fun trimToSize() {
        if (maxBytes <= 0) return
        val urls = fileManager.contentsOfDirectoryAtURL(
            cacheDir,
            includingPropertiesForKeys = null,
            options = 0u,
            error = null,
        ) as? List<NSURL> ?: return

        var total = urls.sumOf { fileSize(it) }
        if (total <= maxBytes) return

        val sorted = urls.sortedBy { modificationDate(it) }
        for (url in sorted) {
            if (total <= maxBytes) break
            val size = fileSize(url)
            tryRemove(url)
            total -= size
        }
    }

    private fun modificationDate(url: NSURL): Double {
        val path = url.path ?: return 0.0
        val attributes = fileManager.attributesOfItemAtPath(path, null)
        val date = attributes?.get(NSFileModificationDate) as? NSDate
        return date?.timeIntervalSince1970 ?: 0.0
    }

    private fun tryRemove(url: NSURL) {
        fileManager.removeItemAtURL(url, null)
    }
}
