package com.yral.shared.app.di

import com.yral.shared.analytics.di.MIXPANEL_TOKEN
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.platform.PlatformResources
import org.koin.dsl.module
import platform.Foundation.NSBundle

private class IOSPlatformResources : PlatformResources

actual val platformModule =
    module {
        single<PlatformResources> { IOSPlatformResources() }
        single { YralLogger(null) }
        single<String>(MIXPANEL_TOKEN) {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("MIXPANEL_TOKEN") as? String)
                ?: error("MIXPANEL_TOKEN missing from Info.plist")
        }
    }
