package com.yral.shared.libs.videoPlayer.prefetch

import co.touchlab.kermit.Logger
import com.yral.shared.libs.videoPlayer.PlatformPlaybackState
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVKeyValueStatusLoaded
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVURLAssetPreferPreciseDurationAndTimingKey
import platform.AVFoundation.automaticallyWaitsToMinimizeStalling
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.volume
import platform.Foundation.NSLock
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.DISPATCH_TIME_FOREVER

internal object IosVideoPrefetchRegistry {
    private val logger = Logger.withTag("iOSPrefetch")
    private val lock = NSLock()
    private val entries = mutableMapOf<String, PrefetchEntry>()

    fun register(
        url: String,
        listener: PrefetchVideoListener?,
        onUrlReady: (String) -> Unit,
    ): PrefetchHandle {
        val entry: PrefetchEntry
        val callback: PrefetchCallback
        lock.lock()
        try {
            entry =
                entries.getOrPut(url) {
                    logger.d { "register: start $url" }
                    PrefetchEntry(url = url, lock = lock).also { it.startPrefetch() }
                }
            callback =
                PrefetchCallback(
                    listener = listener,
                    onUrlReady = onUrlReady,
                )
            entry.callbacks.add(callback)
        } finally {
            lock.unlock()
        }

        entry.notifyInitialState(callback)
        return PrefetchHandleImpl(url, entry, callback)
    }

    fun consume(url: String): AVURLAsset? =
        lock.withLock {
            val entry = entries[url] ?: return null
            logger.d { "consume: using prefetched asset for $url" }
            entry.consumeAsset()
        }

    fun removeCallback(handle: PrefetchHandle) {
        check(handle is PrefetchHandleImpl)
        lock.withLock {
            handle.entry.callbacks.remove(handle.callback)
            logger.d { "removeCallback: removed listener for ${handle.url}" }
        }
    }

    fun evict(url: String) {
        lock.withLock {
            val entry = entries.remove(url) ?: return
            logger.d { "evict: disposing $url" }
            entry.dispose()
        }
    }
    
    private fun removeEntry(url: String, entry: PrefetchEntry) {
        lock.withLock {
            val current = entries[url]
            if (current === entry) {
                entries.remove(url)
            }
        }
    }

    private class PrefetchEntry(
        private val url: String,
        private val lock: NSLock,
    ) {
        private val mainQueue = dispatch_get_main_queue()

        val callbacks = mutableSetOf<PrefetchCallback>()
        var state: PlatformPlaybackState = PlatformPlaybackState.IDLE
            private set
        private var disposed = false
        private val asset: AVURLAsset =
            NSURL.URLWithString(url)
                ?.let { nsUrl ->
                    AVURLAsset(
                        uRL = nsUrl,
                        options = mapOf(AVURLAssetPreferPreciseDurationAndTimingKey to true),
                    )
                } ?: error("Invalid URL: $url")
        private var prefetchPlayer: AVPlayer? = null

        private fun callbacksSnapshot(): List<PrefetchCallback> =
            lock.withLock { callbacks.toList() }

        fun notifyInitialState(callback: PrefetchCallback) {
            dispatch_async(mainQueue) {
                when (state) {
                    PlatformPlaybackState.BUFFERING -> callback.listener?.onBuffer()
                    PlatformPlaybackState.READY -> {
                        callback.listener?.onReady()
                        callback.onUrlReady(url)
                    }
                    PlatformPlaybackState.IDLE -> callback.listener?.onIdle()
                    PlatformPlaybackState.ENDED -> Unit
                }
            }
        }

        @OptIn(ExperimentalForeignApi::class)
        fun startPrefetch() {
            dispatch_async(mainQueue) {
                if (disposed) return@dispatch_async

                state = PlatformPlaybackState.BUFFERING
                callbacksSnapshot().forEach { it.listener?.onBuffer() }
                logger.d { "prefetch: buffering $url" }

                val playerItem = AVPlayerItem(asset = asset)
                prefetchPlayer =
                    AVPlayer(playerItem = playerItem).apply {
                        automaticallyWaitsToMinimizeStalling = true
                        volume = 0f
                        play()
                    }

                asset.loadValuesAsynchronouslyForKeys(listOf("playable")) {
                    val status = asset.statusOfValueForKey("playable", error = null)
                    dispatch_async(mainQueue) {
                        if (disposed) return@dispatch_async
                        if (status == AVKeyValueStatusLoaded) {
                            state = PlatformPlaybackState.READY
                            logger.d { "prefetch: ready $url" }
                            prefetchPlayer?.pause()
                            callbacksSnapshot().forEach { callback ->
                                callback.listener?.onReady()
                                callback.onUrlReady(url)
                            }
                        } else {
                            notifyError()
                        }
                    }
                }
            }
        }

        fun consumeAsset(): AVURLAsset? {
            if (disposed) return null
            runOnMainSync {
                prefetchPlayer?.pause()
                prefetchPlayer?.replaceCurrentItemWithPlayerItem(null)
                prefetchPlayer = null
            }
            logger.d { "prefetch: handoff $url" }
            return asset
        }

        fun dispose() {
            if (disposed) return
            disposed = true
            runOnMainSync {
                prefetchPlayer?.pause()
                prefetchPlayer?.replaceCurrentItemWithPlayerItem(null)
                prefetchPlayer = null
            }
            logger.d { "prefetch: disposed $url" }
            callbacks.clear()
        }

        private fun notifyError() {
            state = PlatformPlaybackState.IDLE
            callbacksSnapshot().forEach { it.listener?.onPlayerError() }
            logger.w { "prefetch: error $url" }
            dispose()
            IosVideoPrefetchRegistry.removeEntry(url, this)
        }
    }

    private class PrefetchHandleImpl(
        val url: String,
        val entry: PrefetchEntry,
        val callback: PrefetchCallback,
    ) : PrefetchHandle {
        override fun dispose() {
            IosVideoPrefetchRegistry.removeCallback(this)
        }
    }

    interface PrefetchHandle {
        fun dispose()
    }

    private data class PrefetchCallback(
        val listener: PrefetchVideoListener?,
        val onUrlReady: (String) -> Unit,
    )
}

private inline fun <T> NSLock.withLock(block: () -> T): T {
    lock()
    return try {
        block()
    } finally {
        unlock()
    }
}

private inline fun <T> runOnMainSync(crossinline block: () -> T): T {
    if (NSThread.isMainThread()) {
        return block()
    }
    var result: T? = null
    val semaphore = dispatch_semaphore_create(0)
    dispatch_async(dispatch_get_main_queue()) {
        result = block()
        dispatch_semaphore_signal(semaphore)
    }
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
    return result as T
}
