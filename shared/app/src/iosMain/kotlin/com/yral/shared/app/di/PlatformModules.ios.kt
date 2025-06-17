package com.yral.shared.app.di

import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.platform.PlatformResources
import org.koin.dsl.module

private class IOSPlatformResources : PlatformResources

actual val platformModule =
    module {
        single<PlatformResources> { IOSPlatformResources() }
        single { YralLogger(null) }
    }
