package com.yral.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.defaultComponentContext
import com.russhwolf.settings.Settings
import com.yral.android.installReferrer.AttributionManager
import com.yral.android.installReferrer.processors.BranchAttributionProcessor
import com.yral.android.update.InAppUpdateManager
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.app.UpdateState
import com.yral.shared.app.isVersionLower
import com.yral.shared.app.nav.DefaultRootComponent
import com.yral.shared.app.ui.MyApplicationTheme
import com.yral.shared.app.ui.screens.RootScreen
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.utils.OAuthResult
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import com.yral.shared.libs.routing.deeplink.engine.RoutingService
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.preferences.stores.AffiliateAttributionStore
import com.yral.shared.rust.service.services.HelperService.initRustLogger
import com.yral.shared.rust.service.services.RustLogLevel
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var oAuthUtils: OAuthUtils
    private lateinit var oAuthUtilsHelper: OAuthUtilsHelper
    private lateinit var rootComponent: DefaultRootComponent
    private var inAppUpdateManager: InAppUpdateManager? = null
    private var mandatoryUpdateSlotShown = false
    private var resumeUpdateCheckJob: Job? = null
    private val crashlyticsManager: CrashlyticsManager by inject()
    private val settings: Settings by inject()
    private val routingService: RoutingService by inject()
    private val affiliateAttributionStore: AffiliateAttributionStore by inject()
    private val attributionManager: AttributionManager by lazy {
        (application as YralApp).getAttributionManager()
    }
    private val branchAttributionProcessor: BranchAttributionProcessor? by lazy {
        (application as YralApp).getBranchAttributionProcessor()
    }

    private val updateResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            inAppUpdateManager?.handleImmediateUpdateResult(result.resultCode)
        }

    // Shared Branch session callback for both init() and reInit()
    private val branchSessionCallback: (BranchUniversalObject?, LinkProperties?, BranchError?) -> Unit =
        { branchUniversalObject, linkProperties, error ->
            if (error != null) {
                Logger.d("BranchSDK") { "branch session error: " + error.message }
            } else {
                Logger.d("BranchSDK") { "branch session complete $intent" }
                if (branchUniversalObject != null) {
                    Logger.d("BranchSDK") { "title " + branchUniversalObject.title }
                    Logger.d("BranchSDK") { "CanonicalIdentifier " + branchUniversalObject.canonicalIdentifier }
                    Logger.d("BranchSDK") { "metadata " + branchUniversalObject.contentMetadata.convertToJson() }
                }
                if (linkProperties != null) {
                    Logger.d("BranchSDK") { "Channel " + linkProperties.channel }
                    Logger.d("BranchSDK") { "control params " + linkProperties.controlParams }
                }
                storeAffiliateAttribution(linkProperties)
                // Store UTM attribution through AttributionManager
                branchAttributionProcessor?.let { processor ->
                    linkProperties?.let { properties ->
                        processor.setLinkProperties(properties)
                        // Clear processed state and re-process to allow Branch processor to run
                        attributionManager.clearProcessedState("Branch")
                        attributionManager.reprocessAttribution()
                    }
                }

                val deeplinkPath =
                    linkProperties?.controlParams?.get("\$deeplink_path")
                        ?: branchUniversalObject?.contentMetadata?.customMetadata?.get("\$deeplink_path")
                deeplinkPath?.let {
                    val appRoute = routingService.parseUrl(it)
                    Logger.d("BranchSDK") { "deeplinkPath $deeplinkPath, appRoute $appRoute" }
                    rootComponent.onNavigationRequest(appRoute)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initRustLogger(if (BuildConfig.DEBUG) RustLogLevel.DEBUG else RustLogLevel.ERROR)
        oAuthUtils = koinInstance.get()
        oAuthUtilsHelper = koinInstance.get()
        // Always create the root component outside Compose on the main thread
        rootComponent = DefaultRootComponent(componentContext = defaultComponentContext())

        val updateManager =
            InAppUpdateManager(
                context = this,
                settings = settings,
                crashlyticsManager = crashlyticsManager,
                onStateChanged = {
                    rootComponent.onUpdateStateChanged(it)
                    if (it is UpdateState.ImmediateUpdateCancelled || it is UpdateState.ImmediateStarted) {
                        finish()
                    }
                },
            )
        inAppUpdateManager = updateManager
        updateManager.setUpdateResultLauncher(updateResultLauncher)

        lifecycleScope.launch {
            if (runRemoteConfigUpdateCheck()) {
                mandatoryUpdateSlotShown = true
                rootComponent.showMandatoryUpdateSlot()
            } else {
                attachInAppUpdateManager(updateManager)
            }
        }

        handleIntent(intent)
        setContent {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
                    MyApplicationTheme {
                        RootScreen(rootComponent)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mandatoryUpdateSlotShown) return
        if (resumeUpdateCheckJob?.isActive == true) return
        resumeUpdateCheckJob =
            lifecycleScope.launch {
                try {
                    if (!runRemoteConfigUpdateCheck(forceRefresh = true)) {
                        mandatoryUpdateSlotShown = false
                        rootComponent.dismissMandatoryUpdateSlot()
                        inAppUpdateManager?.let { attachInAppUpdateManager(it) }
                    }
                } finally {
                    resumeUpdateCheckJob = null
                }
            }
    }

    private fun attachInAppUpdateManager(updateManager: InAppUpdateManager) {
        rootComponent.setOnCompleteUpdateCallback { updateManager.completeUpdate() }
        lifecycle.addObserver(updateManager)
        updateManager.checkForUpdate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update the Activity's intent for branch
        setIntent(intent)
        handleIntent(intent)
        handleIntentForBranch(intent)
    }

    private fun handleIntentForBranch(intent: Intent) {
        if (intent.hasExtra("branch_force_new_session") &&
            intent.getBooleanExtra(
                "branch_force_new_session",
                false,
            )
        ) {
            Branch
                .sessionBuilder(this)
                .withCallback(branchSessionCallback)
                .reInit()
        }
    }

    private fun handleIntent(intent: Intent?) {
        Logger.d("MainActivity") { "onNewIntent: ${intent?.data}" }

        // Handle OAuth redirect URIs
        handleOAuthIntent(intent)?.let {
            oAuthUtils.invokeCallback(it)
            return
        }

        // Handle notification deep links with payload format
        val payload = intent?.getStringExtra("payload")
        if (payload != null) {
            Logger.d("MainActivity") { "Handling notification payload: $payload" }
            mapPayloadToRoute(payload)?.let { route -> handleNotificationDeepLink(route) }
        }
    }

    private fun handleOAuthIntent(intent: Intent?): OAuthResult? =
        intent
            ?.data
            ?.let { oAuthUtilsHelper.mapUriToOAuthResult(it.toString()) }

    private fun mapPayloadToRoute(payload: String): AppRoute? =
        try {
            val jsonObject = Json.decodeFromString(JsonObject.serializer(), payload)
            val internalUrl = jsonObject["internalUrl"]?.jsonPrimitive?.content
            internalUrl?.let { routingService.parseUrl(internalUrl) }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Logger.e("MainActivity", e) { "Error parsing notification payload: $payload" }
            crashlyticsManager.recordException(e)
            null
        }

    private fun handleNotificationDeepLink(dest: AppRoute) {
        try {
            Logger.d("MainActivity") { "Handling deep link: $dest" }
            rootComponent.onNavigationRequest(dest)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Logger.e("MainActivity", e) { "Error handling deep link: $dest" }
            crashlyticsManager.recordException(e)
        }
    }

    override fun onStart() {
        super.onStart()
        initialiseBranch()
    }

    private fun initialiseBranch() {
        Branch
            .sessionBuilder(this)
            .withCallback(branchSessionCallback)
            .withData(this.intent?.data)
            .init()
    }

    override fun onDestroy() {
        oAuthUtils.cleanup()
        super.onDestroy()
    }

    private suspend fun runRemoteConfigUpdateCheck(forceRefresh: Boolean = false): Boolean =
        try {
            val flagManager = koinInstance.get<FeatureFlagManager>()
            if (forceRefresh) {
                flagManager.hydrateAndFetchRemotes()
            } else {
                flagManager.awaitRemoteFetch(5.seconds)
            }
            val config = flagManager.get(AppFeatureFlags.Android.InAppUpdate)
            val currentVersion = BuildConfig.VERSION_NAME
            isVersionLower(currentVersion, config.minSupportedVersion)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Logger.w("MainActivity") { "Remote Config update check failed: ${e.message}" }
            crashlyticsManager.recordException(e)
            false
        }

    private fun storeAffiliateAttribution(linkProperties: LinkProperties?) {
        val rawChannel =
            linkProperties?.let {
                linkProperties.channel
                    ?.takeIf { it.isNotBlank() }
                    ?: linkProperties.controlParams["~channel"]
                    ?: linkProperties.controlParams["channel"]
            }
        val channel = rawChannel?.takeIf { it.isNotBlank() } ?: return
        affiliateAttributionStore.storeIfEmpty(channel)
    }
}
