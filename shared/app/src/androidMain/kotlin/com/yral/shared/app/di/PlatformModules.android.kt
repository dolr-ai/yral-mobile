package com.yral.shared.app.di

import android.content.pm.ApplicationInfo
import co.touchlab.kermit.platformLogWriter
import com.yral.shared.analytics.di.MIXPANEL_TOKEN
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.features.auth.utils.AndroidOAuthUtils
import com.yral.shared.features.auth.utils.AndroidOAuthUtilsHelper
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule =
    module {
        single {
            val isDebug =
                (
                    androidContext().applicationInfo.flags and
                        ApplicationInfo.FLAG_DEBUGGABLE
                ) != 0
            YralLogger(if (isDebug) platformLogWriter() else null)
        }
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
        // Required single
        // Reason: Verified in Repo, Callback in Repo required once app resumes
        single<OAuthUtils> { AndroidOAuthUtils() }
        factory<OAuthUtilsHelper> { AndroidOAuthUtilsHelper() }
    }
