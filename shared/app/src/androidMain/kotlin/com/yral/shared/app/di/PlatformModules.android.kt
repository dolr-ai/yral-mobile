package com.yral.shared.app.di

import android.content.Context
import android.content.pm.ApplicationInfo
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.di.MIXPANEL_TOKEN
import com.yral.shared.features.auth.utils.AndroidOAuthUtils
import com.yral.shared.features.auth.utils.AndroidOAuthUtilsHelper
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.libs.designsystem.component.AndroidScreenFoldStateProvider
import com.yral.shared.libs.designsystem.windowInfo.ScreenFoldStateProvider
import org.koin.android.ext.koin.androidContext
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
        single<Boolean>(IS_DEBUG) {
            (androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }
        // Required single
        // Reason: Verified in Repo, Callback in Repo required once app resumes
        single<OAuthUtils> { AndroidOAuthUtils() }
        factory<OAuthUtilsHelper> { AndroidOAuthUtilsHelper() }
        single<ImageLoader> { SingletonImageLoader.get(get()) }
        factory<ScreenFoldStateProvider> { (activityContext: Context) ->
            AndroidScreenFoldStateProvider(activityContext)
        }
    }
