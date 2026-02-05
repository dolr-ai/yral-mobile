package com.yral.shared.libs.videoplayback

import com.yral.shared.core.logging.YralLogger

interface PlaybackEventReporter {
    fun feedItemImpression(
        id: String,
        index: Int,
    )
    fun playStartRequest(
        id: String,
        index: Int,
        reason: String,
    )
    fun firstFrameRendered(
        id: String,
        index: Int,
    )
    fun timeToFirstFrame(
        id: String,
        index: Int,
        ms: Long,
    )
    fun playbackProgress(
        id: String,
        index: Int,
        positionMs: Long,
        durationMs: Long,
    )
    fun rebufferStart(
        id: String,
        index: Int,
        reason: String,
    )
    fun rebufferEnd(
        id: String,
        index: Int,
        reason: String,
    )
    fun rebufferTotal(
        id: String,
        index: Int,
        ms: Long,
    )
    fun playbackError(
        id: String,
        index: Int,
        category: String,
        code: Any,
        message: String? = null,
    )
    fun playbackEnded(
        id: String,
        index: Int,
    )
    fun preloadScheduled(
        id: String,
        index: Int,
        distance: Int,
        mode: String,
    )
    fun preloadCompleted(
        id: String,
        index: Int,
        bytes: Long,
        ms: Long,
        fromCache: Boolean,
    )
    fun preloadCanceled(
        id: String,
        index: Int,
        reason: String,
    )
    fun cacheHit(
        id: String,
        bytes: Long,
    )
    fun cacheMiss(
        id: String,
        bytes: Long,
    )
}

private const val DEFAULT_PLAYBACK_LOG_TAG = "PlaybackEventReporter"

private data class PlaybackKey(
    val id: String,
    val index: Int,
)

@Suppress("TooManyFunctions")
class LoggingPlaybackEventReporter(
    private val delegate: PlaybackEventReporter,
    yralLogger: YralLogger,
    tag: String = DEFAULT_PLAYBACK_LOG_TAG,
) : PlaybackEventReporter {
    private val logger = yralLogger.withTag(tag)
    private val lastProgressSecondByItem = mutableMapOf<PlaybackKey, Long>()

    override fun feedItemImpression(
        id: String,
        index: Int,
    ) {
        logger.d("feedItemImpression $id $index")
        delegate.feedItemImpression(id, index)
    }

    override fun playStartRequest(
        id: String,
        index: Int,
        reason: String,
    ) {
        logger.d("playStartRequest $id $index $reason")
        delegate.playStartRequest(id, index, reason)
    }

    override fun firstFrameRendered(
        id: String,
        index: Int,
    ) {
        logger.d("firstFrameRendered $id $index")
        delegate.firstFrameRendered(id, index)
    }

    override fun timeToFirstFrame(
        id: String,
        index: Int,
        ms: Long,
    ) {
        logger.d("timeToFirstFrame $id $index $ms")
        delegate.timeToFirstFrame(id, index, ms)
    }

    @Suppress("MagicNumber")
    override fun playbackProgress(
        id: String,
        index: Int,
        positionMs: Long,
        durationMs: Long,
    ) {
        if (positionMs >= 0 && durationMs > 0) {
            val key = PlaybackKey(id, index)
            val second = positionMs / 1000
            val lastSecond = lastProgressSecondByItem[key]
            if (lastSecond == null || lastSecond != second) {
                lastProgressSecondByItem[key] = second
                logger.d("playbackProgress $id $index $positionMs/$durationMs (${second}s)")
            }
        }
        delegate.playbackProgress(id, index, positionMs, durationMs)
    }

    override fun rebufferStart(
        id: String,
        index: Int,
        reason: String,
    ) {
        logger.d("rebufferStart $id $index $reason")
        delegate.rebufferStart(id, index, reason)
    }

    override fun rebufferEnd(
        id: String,
        index: Int,
        reason: String,
    ) {
        logger.d("rebufferEnd $id $index $reason")
        delegate.rebufferEnd(id, index, reason)
    }

    override fun rebufferTotal(
        id: String,
        index: Int,
        ms: Long,
    ) {
        logger.d("rebufferTotal $id $index $ms")
        delegate.rebufferTotal(id, index, ms)
    }

    override fun playbackError(
        id: String,
        index: Int,
        category: String,
        code: Any,
        message: String?,
    ) {
        logger.d("playbackError $id $index $category $code $message")
        delegate.playbackError(id, index, category, code, message)
        lastProgressSecondByItem.remove(PlaybackKey(id, index))
    }

    override fun playbackEnded(
        id: String,
        index: Int,
    ) {
        logger.d("playbackEnded $id $index")
        delegate.playbackEnded(id, index)
        lastProgressSecondByItem.remove(PlaybackKey(id, index))
    }

    override fun preloadScheduled(
        id: String,
        index: Int,
        distance: Int,
        mode: String,
    ) {
        logger.d("preloadScheduled $id $index $distance $mode")
        delegate.preloadScheduled(id, index, distance, mode)
    }

    override fun preloadCompleted(
        id: String,
        index: Int,
        bytes: Long,
        ms: Long,
        fromCache: Boolean,
    ) {
        logger.d("preloadCompleted $id $index $bytes $ms $fromCache")
        delegate.preloadCompleted(id, index, bytes, ms, fromCache)
    }

    override fun preloadCanceled(
        id: String,
        index: Int,
        reason: String,
    ) {
        logger.d("preloadCanceled $id $index $reason")
        delegate.preloadCanceled(id, index, reason)
    }

    override fun cacheHit(
        id: String,
        bytes: Long,
    ) {
        logger.d("cacheHit $id $bytes")
        delegate.cacheHit(id, bytes)
    }

    override fun cacheMiss(
        id: String,
        bytes: Long,
    ) {
        logger.d("cacheMiss $id $bytes")
        delegate.cacheMiss(id, bytes)
    }
}

fun PlaybackEventReporter.withLogging(
    yralLogger: YralLogger,
    enabled: Boolean,
    tag: String = DEFAULT_PLAYBACK_LOG_TAG,
): PlaybackEventReporter = if (enabled) LoggingPlaybackEventReporter(this, yralLogger, tag) else this

@Suppress("TooManyFunctions")
object NoopPlaybackEventReporter : PlaybackEventReporter {
    override fun feedItemImpression(
        id: String,
        index: Int,
    ) = Unit
    override fun playStartRequest(
        id: String,
        index: Int,
        reason: String,
    ) = Unit
    override fun firstFrameRendered(
        id: String,
        index: Int,
    ) = Unit
    override fun timeToFirstFrame(
        id: String,
        index: Int,
        ms: Long,
    ) = Unit
    override fun playbackProgress(
        id: String,
        index: Int,
        positionMs: Long,
        durationMs: Long,
    ) = Unit
    override fun rebufferStart(
        id: String,
        index: Int,
        reason: String,
    ) = Unit
    override fun rebufferEnd(
        id: String,
        index: Int,
        reason: String,
    ) = Unit
    override fun rebufferTotal(
        id: String,
        index: Int,
        ms: Long,
    ) = Unit
    override fun playbackError(
        id: String,
        index: Int,
        category: String,
        code: Any,
        message: String?,
    ) = Unit
    override fun playbackEnded(
        id: String,
        index: Int,
    ) = Unit
    override fun preloadScheduled(
        id: String,
        index: Int,
        distance: Int,
        mode: String,
    ) = Unit
    override fun preloadCompleted(
        id: String,
        index: Int,
        bytes: Long,
        ms: Long,
        fromCache: Boolean,
    ) = Unit
    override fun preloadCanceled(
        id: String,
        index: Int,
        reason: String,
    ) = Unit
    override fun cacheHit(
        id: String,
        bytes: Long,
    ) = Unit
    override fun cacheMiss(
        id: String,
        bytes: Long,
    ) = Unit
}
