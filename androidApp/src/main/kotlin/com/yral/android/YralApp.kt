package com.yral.android

import android.app.Application
import androidx.core.provider.FontRequest
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import co.touchlab.kermit.Logger
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.analytics.providers.mixpanel.MixpanelAnalyticsProvider
import com.yral.shared.app.di.initKoin
import com.yral.shared.features.uploadvideo.utils.di.videoWidgetModule
import com.yral.shared.koin.koinInstance
import dev.gitlive.firebase.crashlytics.FirebaseCrashlytics
import io.branch.referral.Branch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext

class YralApp : Application() {
    private val appCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        setupFacebook()
        setupBranch()
        setupEmoji()
        initKoin {
            androidContext(this@YralApp)
            modules(videoWidgetModule)
        }
        setupFirebase()
        observeAndAddDistinctIdToBranch()
    }

    private fun setupFirebase() {
        koinInstance.get<FirebaseCrashlytics>().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        val firebaseApp = FirebaseApp.getInstance()
        val flagManager = koinInstance.get<FeatureFlagManager>()
        val enableAppCheck = flagManager.isEnabled(AppFeatureFlags.Android.EnableAppCheck)
        if (enableAppCheck) {
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
            Logger.d("YralApp") { "Installed appcheck" }
        } else {
            Logger.d("YralApp") { "Skipping appcheck since it is disabled" }
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
        if (BuildConfig.DEBUG) {
            Branch.enableLogging()
        } else {
            Branch.disableLogging()
        }
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
        Branch.setFBAppID(this.getString(R.string.facebook_app_id))
    }

    private fun observeAndAddDistinctIdToBranch() {
        appCoroutineScope.launch {
            koinInstance.get<MixpanelAnalyticsProvider>().observeDistinctId().collect { distinctId ->
                Logger.d("MixPanel") { "Updating distinct id in branch" }
                Branch.getInstance().setRequestMetadata("\$mixpanel_distinct_id", distinctId)
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
