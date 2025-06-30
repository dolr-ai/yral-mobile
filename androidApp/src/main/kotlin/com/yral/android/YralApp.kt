package com.yral.android

import android.app.Application
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.yral.shared.app.di.initKoin
import com.yral.shared.core.AppConfigurations.FIREBASE_APP_NAME
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext
import com.google.firebase.Firebase as GoogleFirebase
import dev.gitlive.firebase.Firebase as GitLiveFirebase

class YralApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setupFirebase()
        initKoin {
            androidContext(this@YralApp)
        }
    }

    private fun setupFirebase() {
        GitLiveFirebase.initialize(this)
        GitLiveFirebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        val firebaseOptions = FirebaseOptions.fromResource(this)
        if (firebaseOptions == null) {
            return
        }
        val firebaseApp =
            GoogleFirebase.initialize(
                context = this,
                options = firebaseOptions,
                name = FIREBASE_APP_NAME,
            )
        when (BuildConfig.FLAVOR) {
            "prod" -> {
                GoogleFirebase
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
            else -> { }
        }
    }
}
