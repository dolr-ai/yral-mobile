package com.yral.shared.app.di

import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.ShareService
import org.koin.core.scope.Scope
import org.koin.dsl.module

val sharingModule =
    module {
        single<ImageLoader> { SingletonImageLoader.get(get()) }
        factory { createShareService() }
        factory { createLinkGenerator() }
    }

expect fun Scope.createShareService(): ShareService

expect fun Scope.createLinkGenerator(): LinkGenerator
