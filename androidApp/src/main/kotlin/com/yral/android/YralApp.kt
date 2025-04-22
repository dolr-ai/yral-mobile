package com.yral.android

import android.app.Application
import com.yral.android.di.initKoin
import org.koin.android.ext.koin.androidContext

class YralApp : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@YralApp)
        }
    }
}
