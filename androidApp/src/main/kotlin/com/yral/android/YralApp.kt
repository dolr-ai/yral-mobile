package com.yral.android

import android.app.Application
import androidx.core.provider.FontRequest
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
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
        setupEmoji()
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
        Branch.setFBAppID(this.getString(R.string.facebook_app_id))
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

    private fun setupEmoji() {
        // Use Google Play Services downloadable emoji font to avoid APK size increase
        val request =
            FontRequest(
                // providerAuthority =
                "com.google.android.gms.fonts",
                // providerPackage =
                "com.google.android.gms",
                // query =
                "Noto Color Emoji Compat",
                // certificates =
                R.array.com_google_android_gms_fonts_certs,
            )
        val config =
            FontRequestEmojiCompatConfig(this, request)
                .setReplaceAll(true)
        EmojiCompat.init(config)
    }
}
