package com.yral.shared.app.di

import android.content.pm.ApplicationInfo
import co.touchlab.kermit.platformLogWriter
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.platform.AndroidPlatformResources
import com.yral.shared.core.platform.PlatformResources
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule =
    module {
        single<PlatformResources> { AndroidPlatformResources(androidContext()) }
        single {
            val isDebug =
                (
                    androidContext().applicationInfo.flags and
                        ApplicationInfo.FLAG_DEBUGGABLE
                ) != 0
            YralLogger(if (isDebug) platformLogWriter() else null)
        }
    }
