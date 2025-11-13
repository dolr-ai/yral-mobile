package com.yral.shared.app.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.di.MIXPANEL_TOKEN
import com.yral.shared.analytics.di.ONESIGNAL_APP_ID
import com.yral.shared.analytics.providers.onesignal.AndroidOneSignal
import com.yral.shared.analytics.providers.onesignal.OneSignalKMP
import com.yral.shared.crashlytics.di.SENTRY_DSN
import com.yral.shared.crashlytics.di.SENTRY_ENVIRONMENT
import com.yral.shared.crashlytics.di.SENTRY_RELEASE
import com.yral.shared.features.auth.utils.AndroidOAuthUtils
import com.yral.shared.features.auth.utils.AndroidOAuthUtilsHelper
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.libs.designsystem.component.AndroidScreenFoldStateProvider
import com.yral.shared.libs.designsystem.windowInfo.ScreenFoldStateProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule =
    module {
        single<String>(MIXPANEL_TOKEN) {
            androidContext().let {
                it.getString(
                    it.resources.getIdentifier(
                        "mixpanel_token",
                        "string",
                        it.packageName,
                    ),
                )
            }
        }
        single<String>(ONESIGNAL_APP_ID) {
            androidContext().let {
                it.getString(
                    it.resources.getIdentifier(
                        "onesignal_app_id",
                        "string",
                        it.packageName,
                    ),
                )
            }
        }
        single<Boolean>(IS_DEBUG) {
            (androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }
        single<String>(SENTRY_DSN) {
            androidContext().let {
                val id =
                    it.resources.getIdentifier(
                        "sentry_dsn",
                        "string",
                        it.packageName,
                    )
                require(id != 0) { "sentry_dsn string resource missing" }
                it.getString(id).ifBlank { error("sentry_dsn must not be blank") }
            }
        }
        single<String>(SENTRY_ENVIRONMENT) {
            androidContext().let {
                val id =
                    it.resources.getIdentifier(
                        "sentry_environment",
                        "string",
                        it.packageName,
                    )
                if (id == 0) {
                    "android"
                } else {
                    it.getString(id).ifBlank { "android" }
                }
            }
        }
        single<String>(SENTRY_RELEASE) {
            val context = androidContext()
            val packageManager = context.packageManager
            val packageName = context.packageName
            val packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }
            val versionName = packageInfo.versionName ?: "0.0.0"
            val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            "$packageName@$versionName+$versionCode"
        }
        // Required single
        // Reason: Verified in Repo, Callback in Repo required once app resumes
        singleOf(::AndroidOAuthUtils) bind OAuthUtils::class
        factoryOf(::AndroidOAuthUtilsHelper) bind OAuthUtilsHelper::class
        single<ImageLoader> { SingletonImageLoader.get(get()) }
        factory<ScreenFoldStateProvider> { (activityContext: Context) ->
            AndroidScreenFoldStateProvider(activityContext)
        }
        singleOf(::AndroidOneSignal) bind OneSignalKMP::class
    }
