package com.yral.shared.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.yral.shared.analytics.events.SignupNudgeDismissAction
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.ui.components.SignupView
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet
import com.yral.shared.libs.designsystem.theme.YralColors
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginOverlay(
    pageName: SignupPageName,
    tncLink: String,
    headlineText: AnnotatedString? = null,
    disclaimerText: String? = null,
    mode: LoginMode = LoginMode.BOTH,
    onNavigateToCountrySelector: () -> Unit,
    onNavigateToOtpVerification: () -> Unit,
    bottomContent: @Composable () -> Unit = {},
    authTelemetry: AuthTelemetry = koinInject(),
) {
    val termsSheetState = rememberModalBottomSheetState()
    var termsLinkState by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        onDispose {
            authTelemetry.onSignupNudgeDismissed(SignupNudgeDismissAction.SKIP)
        }
    }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(YralColors.ScrimColor)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clickable { },
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(top = 58.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            SignupView(
                pageName = pageName,
                headlineText = headlineText,
                disclaimerText = disclaimerText,
                openTerms = { termsLinkState = tncLink },
                mode = mode,
                onNavigateToCountrySelector = onNavigateToCountrySelector,
                onNavigateToOtpVerification = onNavigateToOtpVerification,
            )
        }
        Column(
            Modifier.weight(1f).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            bottomContent()
        }
    }
    if (termsLinkState.isNotEmpty()) {
        YralWebViewBottomSheet(
            link = termsLinkState,
            bottomSheetState = termsSheetState,
            onDismissRequest = { termsLinkState = "" },
        )
    }
}
