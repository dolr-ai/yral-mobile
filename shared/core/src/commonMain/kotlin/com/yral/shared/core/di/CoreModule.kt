package com.yral.shared.core.di

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.platform.PlatformResourcesFactory
import org.koin.dsl.module

val coreModule =
    module {
        single { PlatformResourcesFactory() }
        single { AppDispatchers() }
    }
