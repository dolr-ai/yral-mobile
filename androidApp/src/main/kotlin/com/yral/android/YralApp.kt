package com.yral.android

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.yral.android.ui.widgets.video.di.videoWidgetModule
import com.yral.shared.app.di.initKoin
import com.yral.shared.koin.koinInstance
import dev.gitlive.firebase.crashlytics.FirebaseCrashlytics
import io.branch.referral.Branch
import org.koin.android.ext.koin.androidContext

class YralApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setupFirebase()
        setupFacebook()
        setupBranch()
        initKoin {
            androidContext(this@YralApp)
            modules(videoWidgetModule)
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

    private fun setupFacebook() {
        if (BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        }
        // Enable Facebook SDK crashlytics enabled
        FacebookSdk.setAutoLogAppEventsEnabled(true)
        FacebookSdk.setAutoInitEnabled(true)
    }

    private fun setupBranch() {
        Branch.enableLogging()
        when (BuildConfig.FLAVOR) {
            "staging" -> {
                Branch.enableTestMode()
                Branch.getAutoInstance(this, BuildConfig.BRANCH_KEY_TEST)
            }
            "prod" -> {
                Branch.disableTestMode()
                Branch.getAutoInstance(this, BuildConfig.BRANCH_KEY)
            }
        }
    }
}
