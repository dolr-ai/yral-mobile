@file:Suppress("MaxLineLength")

package com.yral.shared.app.di

import com.yral.shared.libs.videoPlayer.pool.AndroidPlatformMediaSourceFactory
import com.yral.shared.libs.videoPlayer.pool.AndroidPlatformPlayerFactory
import com.yral.shared.libs.videoPlayer.pool.PlatformMediaSourceFactory
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayerFactory
import org.koin.core.scope.Scope

actual fun Scope.createPlatformPlayerFactory(): PlatformPlayerFactory = AndroidPlatformPlayerFactory(get())

actual fun Scope.createPlatformMediaSourceFactory(): PlatformMediaSourceFactory = AndroidPlatformMediaSourceFactory(get())
