@file:Suppress("EmptyFunctionBlock")

package com.yral.shared.libs.videoPlayer.pool

import com.yral.shared.libs.videoPlayer.PlatformPlayer
import platform.AVFoundation.AVPlayerItem
import platform.Foundation.NSURL

class IosPlatformPlayerFactory : PlatformPlayerFactory {
    override fun createPlayer(): PlatformPlayer = PlatformPlayer()
}

class IosPlatformMediaSourceFactory : PlatformMediaSourceFactory {
    override fun createMediaSource(url: String): Any = AVPlayerItem(uRL = NSURL(string = url))
}
