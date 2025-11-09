package com.yral.shared.app.di

import com.yral.shared.libs.videoPlayer.pool.IosPlatformMediaSourceFactory
import com.yral.shared.libs.videoPlayer.pool.IosPlatformPlayerFactory
import com.yral.shared.libs.videoPlayer.pool.PlatformMediaSourceFactory
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayerFactory
import org.koin.core.scope.Scope

actual fun Scope.createPlatformPlayerFactory(): PlatformPlayerFactory = IosPlatformPlayerFactory()

actual fun Scope.createPlatformMediaSourceFactory(): PlatformMediaSourceFactory = IosPlatformMediaSourceFactory()
