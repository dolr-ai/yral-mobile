package com.yral.android

import android.app.Application
import co.touchlab.kermit.platformLogWriter
import com.yral.shared.core.logging.YralLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class YralApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
        initKoin {
            androidContext(this@YralApp)
        }
    }
}

val platformModule =
    module {
        single {
            YralLogger(
                if (BuildConfig.DEBUG) {
                    platformLogWriter()
                } else {
                    null
                },
            )
        }
    }
