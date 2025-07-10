package com.yral.android

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.yral.shared.app.di.initKoin
import com.yral.shared.koin.koinInstance
import dev.gitlive.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext

class YralApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setupFirebase()
        initKoin {
            androidContext(this@YralApp)
        }
        koinInstance.get<FirebaseCrashlytics>().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun setupFirebase() {
        val firebaseApp = Firebase.initialize(this)
        firebaseApp?.let {
            Firebase
                .appCheck(firebaseApp)
                .installAppCheckProviderFactory {
                    if (BuildConfig.DEBUG) {
                        DebugAppCheckProviderFactory
                            .getInstance()
                            .create(firebaseApp)
                    } else {
                        PlayIntegrityAppCheckProviderFactory
                            .getInstance()
                            .create(firebaseApp)
                    }
                }
        }
    }
}
