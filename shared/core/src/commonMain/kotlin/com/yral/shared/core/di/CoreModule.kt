package com.yral.shared.core.di

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.platform.PlatformResourcesFactory
import com.yral.shared.core.session.SessionManager
import org.koin.dsl.module

val coreModule =
    module {
        single { PlatformResourcesFactory() }
        single { AppDispatchers() }
        single { SessionManager() }
        single { YralLogger(get()) }
    }
