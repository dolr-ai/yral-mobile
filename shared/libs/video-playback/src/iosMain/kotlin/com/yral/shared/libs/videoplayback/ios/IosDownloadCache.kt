package com.yral.shared.libs.videoplayback.ios

import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.cacheKey
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSError
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
    private val session =
        NSURLSession.sessionWithConfiguration(
            NSURLSessionConfiguration.defaultSessionConfiguration,
        )
    private val activeDownloads = mutableMapOf<String, NSURLSessionDownloadTask>()
    private val pendingCallbacks = mutableMapOf<String, MutableList<PendingCallback>>()
    private val lock = SynchronizedObject()

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

    @Suppress("ReturnCount")
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
        val alreadyInProgress =
            synchronized(lock) {
                if (activeDownloads.containsKey(key)) {
                    pendingCallbacks
                        .getOrPut(key) { mutableListOf() }
                        .add(PendingCallback(onComplete, onError))
                    true
                } else {
                    pendingCallbacks[key] = mutableListOf(PendingCallback(onComplete, onError))
                    false
                }
            }
        if (alreadyInProgress) return

        val remoteUrl =
            NSURL.URLWithString(descriptor.uri) ?: run {
                onError()
                return
            }
        val request =
            if (descriptor.headers.isNotEmpty()) {
                NSMutableURLRequest(uRL = remoteUrl).apply {
                    descriptor.headers.forEach { (headerKey, headerValue) ->
                        setValue(headerValue, forHTTPHeaderField = headerKey)
                    }
                }
            } else {
                null
            }
        val task =
            if (request != null) {
                session.downloadTaskWithRequest(request) { tempUrl, _, error ->
                    handleDownloadResult(tempUrl, error, key)
                }
            } else {
                session.downloadTaskWithURL(remoteUrl) { tempUrl, _, error ->
                    handleDownloadResult(tempUrl, error, key)
                }
            }
        synchronized(lock) {
            activeDownloads[key] = task
        }
        task.resume()
    }

    private fun handleDownloadResult(
        tempUrl: NSURL?,
        error: NSError?,
        key: String,
    ) {
        val callbacks =
            synchronized(lock) {
                activeDownloads.remove(key)
                pendingCallbacks.remove(key).orEmpty()
            }
        if (error != null || tempUrl == null) {
            callbacks.forEach { it.onError() }
            return
        }
        val destination = cacheDir.URLByAppendingPathComponent("$key.mp4")
        if (destination == null) {
            callbacks.forEach { it.onError() }
            return
        }
        moveToCache(tempUrl, destination)
        val bytes = fileSize(destination)
        trimToSize()
        callbacks.forEach { it.onComplete(bytes, false) }
    }

    fun cancelPrefetch(descriptor: MediaDescriptor) {
        val key = descriptor.cacheKey()
        val (task, callbacks) =
            synchronized(lock) {
                activeDownloads.remove(key) to pendingCallbacks.remove(key).orEmpty()
            }
        task?.cancel()
        callbacks.forEach { it.onError() }
    }

    fun cancelAll() {
        val entries =
            synchronized(lock) {
                val tasks =
                    activeDownloads.map { (key, task) ->
                        task to pendingCallbacks.remove(key).orEmpty()
                    }
                activeDownloads.clear()
                pendingCallbacks.clear()
                tasks
            }
        entries.forEach { (task, callbacks) ->
            task.cancel()
            callbacks.forEach { it.onError() }
        }
    }

    private fun ensureCacheDir(): NSURL {
        val urls = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
        val base =
            urls.firstOrNull() as? NSURL
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

    private fun moveToCache(
        tempUrl: NSURL,
        destination: NSURL,
    ) {
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

    private data class PendingCallback(
        val onComplete: (bytes: Long, fromCache: Boolean) -> Unit,
        val onError: () -> Unit,
    )

    @Suppress("ReturnCount")
    private fun trimToSize() {
        if (maxBytes <= 0) return
        val urls =
            fileManager.contentsOfDirectoryAtURL(
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
