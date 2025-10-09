package com.yral.shared.app.di

import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.di.MIXPANEL_TOKEN
import com.yral.shared.features.auth.utils.IosOAuthUtils
import com.yral.shared.features.auth.utils.IosOAuthUtilsHelper
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.libs.designsystem.component.IOSScreenFoldStateProvider
import com.yral.shared.libs.designsystem.windowInfo.ScreenFoldStateProvider
import org.koin.dsl.module
import platform.Foundation.NSBundle

actual val platformModule =
    module {
        single<String>(MIXPANEL_TOKEN) {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("MIXPANEL_TOKEN") as? String)
                ?: error("MIXPANEL_TOKEN missing from Info.plist")
        }
        single<Boolean>(IS_DEBUG) {
            NSBundle.mainBundle.bundleIdentifier != "com.yral.iosApp"
        }
        single<OAuthUtils> { IosOAuthUtils() }
        factory<OAuthUtilsHelper> { IosOAuthUtilsHelper() }
        single<ScreenFoldStateProvider> { IOSScreenFoldStateProvider() }
    }
