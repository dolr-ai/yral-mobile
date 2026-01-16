package com.yral.shared.libs.videoplayback

interface PlaybackCoordinator {
    fun setFeed(items: List<MediaDescriptor>)
    fun appendFeed(items: List<MediaDescriptor>)

    fun setActiveIndex(index: Int)

    fun setScrollHint(predictedIndex: Int, velocity: Float? = null)

    fun bindSurface(index: Int, surface: VideoSurfaceHandle)

    fun unbindSurface(index: Int)

    fun onAppForeground()

    fun onAppBackground()

    fun release()
}
