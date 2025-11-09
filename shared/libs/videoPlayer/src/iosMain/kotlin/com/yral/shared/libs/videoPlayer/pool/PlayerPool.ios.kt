@file:Suppress("EmptyFunctionBlock")

package com.yral.shared.libs.videoPlayer.pool

import co.touchlab.kermit.Logger
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.prefetch.IosVideoPrefetchRegistry
import platform.AVFoundation.AVPlayerItem
import platform.Foundation.NSURL

class IosPlatformPlayerFactory : PlatformPlayerFactory {
    override fun createPlayer(): PlatformPlayer = PlatformPlayer()
}

class IosPlatformMediaSourceFactory : PlatformMediaSourceFactory {
    private val logger = Logger.withTag("iOSPrefetch")

    override fun createMediaSource(url: String): Any {
        IosVideoPrefetchRegistry.consume(url)?.let { prefetchedAsset ->
            logger.d { "mediaSource: using prefetched asset for $url" }
            return AVPlayerItem(asset = prefetchedAsset)
        }
        val nsUrl =
            requireNotNull(NSURL(string = url)) {
                "Invalid URL supplied to media source factory: $url"
            }
        logger.d { "mediaSource: creating fresh item for $url" }
        return AVPlayerItem(uRL = nsUrl)
    }
}
