package com.yral.shared.app.di

import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.ShareService
import org.koin.core.scope.Scope
import org.koin.dsl.module

val sharingModule =
    module {
        factory { createShareService() }
        factory { createLinkGenerator() }
    }

expect fun Scope.createShareService(): ShareService

expect fun Scope.createLinkGenerator(): LinkGenerator
