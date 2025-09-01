package com.yral.shared.app.di

import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.di.MIXPANEL_TOKEN
import com.yral.shared.core.logging.YralLogger
import org.koin.dsl.module
import platform.Foundation.NSBundle

actual val platformModule =
    module {
        single { YralLogger(null) }
        single<String>(MIXPANEL_TOKEN) {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("MIXPANEL_TOKEN") as? String)
                ?: error("MIXPANEL_TOKEN missing from Info.plist")
        }
        single<Boolean>(IS_DEBUG) {
            NSBundle.mainBundle.bundleIdentifier != "com.yral.iosApp"
        }
    }
