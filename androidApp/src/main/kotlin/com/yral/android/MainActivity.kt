package com.yral.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.defaultComponentContext
import com.russhwolf.settings.Settings
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.appTypoGraphy
import com.yral.android.ui.nav.DefaultRootComponent
import com.yral.android.ui.screens.RootScreen
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.android.update.InAppUpdateManager
import com.yral.android.update.UpdateState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.utils.OAuthResult
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.routing.deeplink.engine.RoutingService
import com.yral.shared.rust.service.services.HelperService.initRustLogger
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
        if (BuildConfig.DEBUG) {
            initRustLogger()
        }
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
        if (intent.hasExtra("branch_force_new_session") && intent.getBooleanExtra("branch_force_new_session", false)) {
            Branch
                .sessionBuilder(this)
                .withCallback(branchSessionCallback)
                .reInit()
        }
    }

    private fun handleIntent(intent: Intent?) {
        Logger.d("onNewIntent: ${intent?.data}")

        // Handle OAuth redirect URIs
        handleOAuthIntent(intent)?.let {
            oAuthUtils.invokeCallback(it)
            return
        }

        // Handle notification deep links with payload format
        val payload = intent?.getStringExtra("payload")
        if (payload != null) {
            Logger.d("MainActivity") { "Handling notification payload: $payload" }
            val destination = mapPayloadToDestination(payload)
            if (destination != null) {
                handleNotificationDeepLink(destination)
            }
        }
    }

    private fun handleOAuthIntent(intent: Intent?): OAuthResult? =
        intent
            ?.data
            ?.let { oAuthUtilsHelper.mapUriToOAuthResult(it.toString()) }

    private fun mapPayloadToDestination(payload: String): String? =
        try {
            val jsonObject = Json.decodeFromString(JsonObject.serializer(), payload)
            val type = jsonObject["type"]?.jsonPrimitive?.content

            when (type) {
                "VideoUploadSuccessful" -> {
                    // For video upload success, navigate to specific post
                    val videoId = jsonObject["video_id"]?.jsonPrimitive?.content

                    if (!videoId.isNullOrEmpty()) {
                        "${ProfileComponent.DEEPLINK_VIDEO_PREFIX}/$videoId"
                    } else {
                        ProfileComponent.DEEPLINK
                    }
                }
                // Add more notification types here as needed
                else -> {
                    Logger.w("MainActivity") { "Unknown notification type: $type" }
                    null
                }
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Logger.e("MainActivity", e) { "Error parsing notification payload: $payload" }
            crashlyticsManager.recordException(e)
            null
        }

    private fun handleNotificationDeepLink(dest: String) {
        try {
            Logger.d("MainActivity") { "Handling deep link: $dest" }
            rootComponent.handleNavigation(dest)
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
}

@Suppress("MagicNumber")
@Composable
private fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            darkColorScheme(
                primary = YralColors.Pink300,
                secondary = Color(0xFF03DAC5),
                tertiary = Color(0xFF3700B3),
                primaryContainer = YralColors.PrimaryContainer,
                onPrimaryContainer = YralColors.OnPrimaryContainer,
            )
        } else {
            lightColorScheme(
                primary = YralColors.Pink300,
                secondary = Color(0xFF03DAC5),
                tertiary = Color(0xFF3700B3),
                primaryContainer = YralColors.PrimaryContainer,
                onPrimaryContainer = YralColors.OnPrimaryContainer,
            )
        }
    val typography =
        Typography(
            bodyMedium =
                TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                ),
        )
    val shapes =
        Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(0.dp),
        )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
