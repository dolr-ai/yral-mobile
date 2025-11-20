package com.yral.shared.app.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.di.MIXPANEL_TOKEN
import com.yral.shared.analytics.di.ONESIGNAL_APP_ID
import com.yral.shared.crashlytics.di.SENTRY_DSN
import com.yral.shared.crashlytics.di.SENTRY_ENVIRONMENT
import com.yral.shared.crashlytics.di.SENTRY_RELEASE
import com.yral.shared.features.auth.utils.IosOAuthUtils
import com.yral.shared.features.auth.utils.IosOAuthUtilsHelper
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.libs.designsystem.component.IOSScreenFoldStateProvider
import com.yral.shared.libs.designsystem.windowInfo.ScreenFoldStateProvider
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.Foundation.NSBundle

actual val platformModule =
    module {
        single<String>(MIXPANEL_TOKEN) {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("MIXPANEL_TOKEN") as? String)
                ?: error("MIXPANEL_TOKEN missing from Info.plist")
        }
        single<String>(ONESIGNAL_APP_ID) {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("ONESIGNAL_APP_ID") as? String)
                ?: error("ONESIGNAL_APP_ID missing from Info.plist")
        }
        single<Boolean>(IS_DEBUG) {
            NSBundle.mainBundle.bundleIdentifier != "com.yral.iosApp"
        }
        single<String>(SENTRY_DSN) {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("SENTRY_DSN") as? String)
                ?: error("SENTRY_DSN missing from Info.plist")
        }
        single<String>(SENTRY_ENVIRONMENT) {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("SENTRY_ENVIRONMENT") as? String)
                ?: "ios: unknown"
        }
        single<String>(SENTRY_RELEASE) {
            val bundle = NSBundle.mainBundle
            val bundleId = bundle.bundleIdentifier ?: "com.yral.iosApp"
            val version =
                (bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
                    ?: "0.0.0"
            val buildNumber = (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String) ?: "0"
            "$bundleId@$version+$buildNumber"
        }
        singleOf(::IosOAuthUtils) bind OAuthUtils::class
        factoryOf(::IosOAuthUtilsHelper) bind OAuthUtilsHelper::class
        single<ScreenFoldStateProvider> { IOSScreenFoldStateProvider() }
        single<ImageLoader> { SingletonImageLoader.get(PlatformContext.INSTANCE) }
    }
