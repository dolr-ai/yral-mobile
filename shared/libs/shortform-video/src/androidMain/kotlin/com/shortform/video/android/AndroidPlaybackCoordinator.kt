package com.shortform.video.android

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.database.ExoDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.dash.DashMediaSource
import androidx.media3.exoplayer.source.hls.HlsMediaSource
import com.shortform.video.ContainerHint
import com.shortform.video.CoordinatorDeps
import com.shortform.video.MediaDescriptor
import com.shortform.video.PlaybackCoordinator
import com.shortform.video.VideoSurfaceHandle
import com.shortform.video.ui.AndroidVideoSurfaceHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File

fun createAndroidPlaybackCoordinator(
    context: Context,
    deps: CoordinatorDeps = CoordinatorDeps(),
): PlaybackCoordinator = AndroidPlaybackCoordinator(context, deps)

private class AndroidPlaybackCoordinator(
    context: Context,
    private val deps: CoordinatorDeps,
) : PlaybackCoordinator {
    private val appContext = context.applicationContext
    private val analytics = deps.analytics
    private val policy = deps.policy
    private val nowMs = deps.nowMs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cache = createCache(appContext)
    private val cacheKeyFactory = CacheKeyFactory.DEFAULT
    private val dataSourceFactory = createCacheDataSourceFactory(appContext, cache)
    private val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

    private val playerA = createPlayer(appContext)
    private val playerB = createPlayer(appContext)
    private var activeSlot = PlayerSlot(playerA)
    private var preparedSlot = PlayerSlot(playerB)

    private val surfaces = mutableMapOf<Int, VideoSurfaceHandle>()
    private var feed: List<MediaDescriptor> = emptyList()
    private var activeIndex: Int = -1
    private var predictedIndex: Int = -1

    private val playStartMsById = mutableMapOf<String, Long>()
    private var firstFramePendingIndex: Int? = null
    private var rebuffering = false

    private val prefetchJobs = mutableMapOf<Int, Job>()
    private val prefetchSemaphore = Semaphore(policy.maxConcurrentPrefetch)

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val index = activeSlot.index ?: return
            val item = feed.getOrNull(index) ?: return

            if (playbackState == Player.STATE_BUFFERING && activeSlot.player.playWhenReady) {
                if (!rebuffering) {
                    rebuffering = true
                    analytics.event(
                        "rebuffer_start",
                        mapOf("id" to item.id, "index" to index, "reason" to "buffering"),
                    )
                }
            }

            if (playbackState == Player.STATE_READY && rebuffering) {
                rebuffering = false
                analytics.event(
                    "rebuffer_end",
                    mapOf("id" to item.id, "index" to index, "reason" to "buffering"),
                )
            }

            if (playbackState == Player.STATE_ENDED) {
                analytics.event("playback_ended", mapOf("id" to item.id, "index" to index))
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val index = activeSlot.index ?: return
            val item = feed.getOrNull(index) ?: return
            analytics.event(
                "playback_error",
                mapOf(
                    "id" to item.id,
                    "index" to index,
                    "category" to error.errorCodeName,
                    "code" to error.errorCode,
                    "message" to (error.message ?: ""),
                ),
            )
        }

        override fun onRenderedFirstFrame() {
            val index = activeSlot.index ?: return
            val item = feed.getOrNull(index) ?: return
            if (firstFramePendingIndex != index) return
            firstFramePendingIndex = null
            val start = playStartMsById[item.id] ?: return
            val elapsed = nowMs() - start
            analytics.event("first_frame_rendered", mapOf("id" to item.id, "index" to index))
            analytics.timing("time_to_first_frame_ms", elapsed, mapOf("id" to item.id, "index" to index))
        }
    }

    init {
        playerA.addListener(listener)
        playerB.addListener(listener)
    }

    override fun setFeed(items: List<MediaDescriptor>) {
        feed = items
        if (activeIndex >= items.size) {
            setActiveIndex(items.lastIndex)
        }
    }

    override fun setActiveIndex(index: Int) {
        if (index !in feed.indices) return
        if (index == activeIndex && activeSlot.index == index) return

        activeIndex = index
        predictedIndex = index
        val item = feed[index]

        analytics.event("feed_item_impression", mapOf("id" to item.id, "index" to index))
        analytics.event("play_start_request", mapOf("id" to item.id, "index" to index, "reason" to "activeIndex"))
        playStartMsById[item.id] = nowMs()
        firstFramePendingIndex = index

        if (preparedSlot.index == index) {
            swapSlots()
        } else {
            prepareSlot(activeSlot, index, playWhenReady = true)
        }

        attachIfBound(activeSlot)
        activeSlot.player.playWhenReady = true

        schedulePreparedSlot(index)
        scheduleDiskPrefetch(index)
    }

    override fun setScrollHint(predictedIndex: Int, velocity: Float?) {
        if (predictedIndex !in feed.indices) return
        if (predictedIndex == this.predictedIndex) return
        this.predictedIndex = predictedIndex
        scheduleDiskPrefetch(predictedIndex)
    }

    override fun bindSurface(index: Int, surface: VideoSurfaceHandle) {
        surfaces[index] = surface
        if (activeSlot.index == index) {
            attachIfBound(activeSlot)
        } else if (preparedSlot.index == index) {
            attachIfBound(preparedSlot)
        }
    }

    override fun unbindSurface(index: Int) {
        val handle = surfaces.remove(index)
        if (handle is AndroidVideoSurfaceHandle) {
            if (activeSlot.index == index && handle.playerView.player == activeSlot.player) {
                handle.playerView.player = null
            }
            if (preparedSlot.index == index && handle.playerView.player == preparedSlot.player) {
                handle.playerView.player = null
            }
        }
    }

    override fun onAppForeground() {
        if (activeSlot.index in feed.indices) {
            activeSlot.player.playWhenReady = true
        }
    }

    override fun onAppBackground() {
        activeSlot.player.playWhenReady = false
        preparedSlot.player.playWhenReady = false
        cancelPrefetch(reason = "background")
    }

    override fun release() {
        cancelPrefetch(reason = "release")
        scope.cancel()
        ioScope.cancel()
        playerA.release()
        playerB.release()
        cache.release()
    }

    private fun schedulePreparedSlot(activeIndex: Int) {
        val nextIndex = activeIndex + 1
        if (nextIndex in feed.indices) {
            if (preparedSlot.index != nextIndex) {
                prepareSlot(preparedSlot, nextIndex, playWhenReady = false)
                analytics.event(
                    "preload_scheduled",
                    mapOf("id" to feed[nextIndex].id, "index" to nextIndex, "distance" to 1, "mode" to "prepared"),
                )
            }
        }
    }

    private fun scheduleDiskPrefetch(centerIndex: Int) {
        val start = centerIndex + 1
        val end = centerIndex + policy.diskPrefetchNext
        val desired = (start..end).filter { it in feed.indices }
        val desiredSet = desired.toSet()

        val toCancel = prefetchJobs.keys.filter { it !in desiredSet }
        for (index in toCancel) {
            cancelPrefetch(index, "window_shift")
        }

        for (index in desired) {
            if (prefetchJobs.containsKey(index)) continue
            val item = feed[index]
            analytics.event(
                "preload_scheduled",
                mapOf("id" to item.id, "index" to index, "distance" to (index - centerIndex), "mode" to "disk"),
            )
            val job = ioScope.launch {
                prefetchSemaphore.withPermit {
                    val dataSpec = buildPrefetchDataSpec(item)
                    val cacheKey = dataSpec.key ?: cacheKeyFactory.buildCacheKey(dataSpec)
                    val cachedBytes = cache.getCachedBytes(cacheKey, 0, policy.preloadTargetBytes)
                    if (cachedBytes > 0) {
                        analytics.event("cache_hit", mapOf("id" to item.id, "bytes" to cachedBytes))
                    } else {
                        analytics.event("cache_miss", mapOf("id" to item.id, "bytes" to policy.preloadTargetBytes))
                    }

                    val startMs = nowMs()
                    try {
                        val bytes = cachePrefetch(item)
                        analytics.event(
                            "preload_completed",
                            mapOf(
                                "id" to item.id,
                                "index" to index,
                                "bytes" to bytes,
                                "ms" to (nowMs() - startMs),
                                "fromCache" to (bytes <= cachedBytes),
                            ),
                        )
                    } catch (_: Throwable) {
                        analytics.event(
                            "preload_canceled",
                            mapOf("id" to item.id, "index" to index, "reason" to "error"),
                        )
                    }
                }
            }
            prefetchJobs[index] = job
            job.invokeOnCompletion { throwable ->
                prefetchJobs.remove(index)
            }
        }
    }

    private fun cancelPrefetch(index: Int, reason: String) {
        prefetchJobs.remove(index)?.cancel()
        feed.getOrNull(index)?.let { item ->
            analytics.event("preload_canceled", mapOf("id" to item.id, "index" to index, "reason" to reason))
        }
    }

    private fun cancelPrefetch(reason: String) {
        val indices = prefetchJobs.keys.toList()
        for (index in indices) {
            cancelPrefetch(index, reason)
        }
    }

    private fun prepareSlot(slot: PlayerSlot, index: Int, playWhenReady: Boolean) {
        val item = feed[index]
        val source = buildMediaSource(item)
        slot.index = index
        slot.player.setMediaSource(source)
        slot.player.prepare()
        slot.player.playWhenReady = playWhenReady
    }

    private fun swapSlots() {
        val previousActive = activeSlot
        activeSlot = preparedSlot
        preparedSlot = previousActive
    }

    private fun attachIfBound(slot: PlayerSlot) {
        val index = slot.index ?: return
        val handle = surfaces[index]
        if (handle is AndroidVideoSurfaceHandle) {
            if (handle.playerView.player != slot.player) {
                handle.playerView.player = slot.player
            }
        }
    }

    private fun buildMediaSource(descriptor: MediaDescriptor): MediaSource {
        val item = buildMediaItem(descriptor)
        return when (descriptor.containerHint) {
            ContainerHint.HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item)
            ContainerHint.DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(item)
            ContainerHint.MP4 -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item)
            ContainerHint.UNKNOWN -> mediaSourceFactory.createMediaSource(item)
        }
    }

    private fun buildMediaItem(descriptor: MediaDescriptor): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(descriptor.uri)
            .setMediaId(descriptor.id)

        when (descriptor.containerHint) {
            ContainerHint.MP4 -> builder.setMimeType(MimeTypes.APPLICATION_MP4)
            ContainerHint.HLS -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            ContainerHint.DASH -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            ContainerHint.UNKNOWN -> Unit
        }

        return builder.build()
    }

    private suspend fun cachePrefetch(descriptor: MediaDescriptor): Long {
        val dataSource = dataSourceFactory.createDataSource()
        val dataSpec = buildPrefetchDataSpec(descriptor)
        var latestBytes = 0L
        val cacheWriter = CacheWriter(
            dataSource,
            dataSpec,
            null,
            CacheWriter.ProgressListener { _, bytesCached, _ ->
                latestBytes = bytesCached
            },
        )
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cacheWriter.cancel() }
            try {
                cacheWriter.cache()
                continuation.resume(latestBytes)
            } catch (throwable: Throwable) {
                continuation.resumeWithException(throwable)
            }
        }
    }

    private fun createPlayer(context: Context): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)
            }
    }

    private fun createCache(context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "shortform-video-cache")
        val evictor = LeastRecentlyUsedCacheEvictor(policy.cacheMaxBytes)
        return SimpleCache(cacheDir, evictor, ExoDatabaseProvider(context))
    }

    private fun createCacheDataSourceFactory(context: Context, cache: SimpleCache): CacheDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private data class PlayerSlot(
        val player: ExoPlayer,
        var index: Int? = null,
    )

    private fun buildPrefetchDataSpec(descriptor: MediaDescriptor): DataSpec {
        val cacheKey = cacheKeyFactory.buildCacheKey(
            DataSpec.Builder()
                .setUri(descriptor.uri)
                .setPosition(0)
                .setLength(policy.preloadTargetBytes)
                .build(),
        )
        return DataSpec.Builder()
            .setUri(descriptor.uri)
            .setPosition(0)
            .setLength(policy.preloadTargetBytes)
            .setKey(cacheKey)
            .build()
    }
}
