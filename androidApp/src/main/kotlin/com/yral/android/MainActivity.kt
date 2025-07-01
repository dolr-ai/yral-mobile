package com.yral.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.appTypoGraphy
import com.yral.android.ui.screens.RootScreen
import com.yral.android.ui.widgets.video.VideoPermissionUtils
import com.yral.shared.core.platform.AndroidPlatformResources
import com.yral.shared.core.platform.PlatformResourcesFactory
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_HOST
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_PATH
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_SCHEME
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.koin.koinInstance
import com.yral.shared.uniffi.generated.initRustLogger

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var oAuthUtils: OAuthUtils
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Logger.d("Permissions granted - video player can access files")
            } else {
                Logger.d("Permissions denied - handle accordingly")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initPlatformResources()
        if (BuildConfig.DEBUG) {
            initRustLogger()
        }
        oAuthUtils = koinInstance.get()
        handleIntent(intent)

        checkAndRequestVideoPermissions()

        setContent {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
                    MyApplicationTheme {
                        RootScreen()
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
    }

    private fun checkAndRequestVideoPermissions() {
        val permissionsToRequest = VideoPermissionUtils.getMissingVideoPermissions(this)
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
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
