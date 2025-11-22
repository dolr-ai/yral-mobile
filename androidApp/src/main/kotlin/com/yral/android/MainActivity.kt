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
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.defaultComponentContext
import com.russhwolf.settings.Settings
import com.yral.android.update.InAppUpdateManager
import com.yral.shared.app.UpdateState
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
import com.yral.shared.preferences.AffiliateAttributionStore
import com.yral.shared.preferences.UTM_CAMPAIGN_PARAM
import com.yral.shared.preferences.UTM_CONTENT_PARAM
import com.yral.shared.preferences.UTM_MEDIUM_PARAM
import com.yral.shared.preferences.UTM_SOURCE_PARAM
import com.yral.shared.preferences.UTM_TERM_PARAM
import com.yral.shared.preferences.UtmAttributionStore
import com.yral.shared.rust.service.services.HelperService.initRustLogger
import com.yral.shared.rust.service.services.RustLogLevel
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.util.LinkProperties
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.android.ext.android.inject

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var oAuthUtils: OAuthUtils
    private lateinit var oAuthUtilsHelper: OAuthUtilsHelper
    private lateinit var rootComponent: DefaultRootComponent
    private lateinit var inAppUpdateManager: InAppUpdateManager
    private val crashlyticsManager: CrashlyticsManager by inject()
    private val settings: Settings by inject()
    private val routingService: RoutingService by inject()
    private val affiliateAttributionStore: AffiliateAttributionStore by inject()
    private val utmAttributionStore: UtmAttributionStore by inject()

    private val updateResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            inAppUpdateManager.handleImmediateUpdateResult(result.resultCode)
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
                storeUtmAttribution(linkProperties)

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

        // Initialize in-app update manager
        inAppUpdateManager =
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
        inAppUpdateManager.setUpdateResultLauncher(updateResultLauncher)
        rootComponent.setOnCompleteUpdateCallback { inAppUpdateManager.completeUpdate() }
        lifecycle.addObserver(inAppUpdateManager)

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

    private fun storeUtmAttribution(linkProperties: LinkProperties?) {
        val controlParams = linkProperties?.controlParams ?: return
        Logger.d("BranchSDK") { "controlParams: $controlParams" }
        val utmSource = controlParams[UTM_SOURCE_PARAM]
        val utmMedium = controlParams[UTM_MEDIUM_PARAM]
        val utmCampaign = controlParams[UTM_CAMPAIGN_PARAM]
        val utmTerm = controlParams[UTM_TERM_PARAM]
        val utmContent = controlParams[UTM_CONTENT_PARAM]

        utmAttributionStore.storeIfEmpty(
            source = utmSource,
            medium = utmMedium,
            campaign = utmCampaign,
            term = utmTerm,
            content = utmContent,
        )
    }
}
