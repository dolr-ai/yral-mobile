package com.yral.shared.libs.videoplayback

interface PlaybackEventReporter {
    fun feedItemImpression(id: String, index: Int)
    fun playStartRequest(id: String, index: Int, reason: String)
    fun firstFrameRendered(id: String, index: Int)
    fun timeToFirstFrame(id: String, index: Int, ms: Long)
    fun playbackProgress(id: String, index: Int, positionMs: Long, durationMs: Long)
    fun rebufferStart(id: String, index: Int, reason: String)
    fun rebufferEnd(id: String, index: Int, reason: String)
    fun rebufferTotal(id: String, index: Int, ms: Long)
    fun playbackError(id: String, index: Int, category: String, code: Any, message: String? = null)
    fun playbackEnded(id: String, index: Int)
    fun preloadScheduled(id: String, index: Int, distance: Int, mode: String)
    fun preloadCompleted(id: String, index: Int, bytes: Long, ms: Long, fromCache: Boolean)
    fun preloadCanceled(id: String, index: Int, reason: String)
    fun cacheHit(id: String, bytes: Long)
    fun cacheMiss(id: String, bytes: Long)
}

object NoopPlaybackEventReporter : PlaybackEventReporter {
    override fun feedItemImpression(id: String, index: Int) = Unit
    override fun playStartRequest(id: String, index: Int, reason: String) = Unit
    override fun firstFrameRendered(id: String, index: Int) = Unit
    override fun timeToFirstFrame(id: String, index: Int, ms: Long) = Unit
    override fun playbackProgress(id: String, index: Int, positionMs: Long, durationMs: Long) = Unit
    override fun rebufferStart(id: String, index: Int, reason: String) = Unit
    override fun rebufferEnd(id: String, index: Int, reason: String) = Unit
    override fun rebufferTotal(id: String, index: Int, ms: Long) = Unit
    override fun playbackError(id: String, index: Int, category: String, code: Any, message: String?) = Unit
    override fun playbackEnded(id: String, index: Int) = Unit
    override fun preloadScheduled(id: String, index: Int, distance: Int, mode: String) = Unit
    override fun preloadCompleted(id: String, index: Int, bytes: Long, ms: Long, fromCache: Boolean) = Unit
    override fun preloadCanceled(id: String, index: Int, reason: String) = Unit
    override fun cacheHit(id: String, bytes: Long) = Unit
    override fun cacheMiss(id: String, bytes: Long) = Unit
}
