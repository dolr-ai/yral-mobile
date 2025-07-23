package com.yral.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.appTypoGraphy
import com.yral.android.ui.nav.DefaultRootComponent
import com.yral.android.ui.screens.RootScreen
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.core.platform.AndroidPlatformResources
import com.yral.shared.core.platform.PlatformResourcesFactory
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_HOST
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_PATH
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_SCHEME
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.koin.koinInstance
import com.yral.shared.uniffi.generated.initRustLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var oAuthUtils: OAuthUtils
    private lateinit var rootComponent: DefaultRootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initPlatformResources()
        if (BuildConfig.DEBUG) {
            initRustLogger()
        }
        oAuthUtils = koinInstance.get()
        // Always create the root component outside Compose on the main thread
        rootComponent = DefaultRootComponent(componentContext = defaultComponentContext())
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

    private fun initPlatformResources() {
        koinInstance
            .get<PlatformResourcesFactory>()
            .initialize(AndroidPlatformResources(this))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Logger.d("onNewIntent: ${intent?.data}")

        // Handle OAuth redirect URIs
        val uri = intent?.data
        if (uri?.scheme == REDIRECT_URI_SCHEME &&
            uri.host == REDIRECT_URI_HOST &&
            uri.path == REDIRECT_URI_PATH
        ) {
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            if (code != null && state != null) {
                oAuthUtils.invokeCallback(code, state)
            }
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
        }
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
