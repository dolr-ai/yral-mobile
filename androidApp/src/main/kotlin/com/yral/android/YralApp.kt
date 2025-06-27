package com.yral.android

import android.app.Application
import com.yral.shared.app.di.initKoin
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext

class YralApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        initKoin {
            androidContext(this@YralApp)
        }
    }
}
