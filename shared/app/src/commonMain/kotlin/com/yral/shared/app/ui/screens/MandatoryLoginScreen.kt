package com.yral.shared.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.nav.mandatorylogin.MandatoryLoginComponent
import com.yral.shared.features.auth.ui.components.SignupView
import com.yral.shared.features.auth.ui.components.getAnnotatedHeaderForLogin
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet
import com.yral.shared.libs.designsystem.windowInfo.rememberScreenFoldStateProvider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.login_bottom_shadow
import yral_mobile.shared.app.generated.resources.login_coins
import yral_mobile.shared.app.generated.resources.login_to_unlock_tokens
import yral_mobile.shared.app.generated.resources.login_top_shadow
import yral_mobile.shared.app.generated.resources.reward_tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryLoginScreen(
    modifier: Modifier,
    component: MandatoryLoginComponent,
    loginViewModel: LoginViewModel,
) {
    val termsSheetState = rememberModalBottomSheetState()
    var termsLinkState by remember { mutableStateOf("") }
    val fullText =
        stringResource(
            Res.string.login_to_unlock_tokens,
            loginViewModel.getInitialBalanceReward(),
        )
    val rewardText =
        stringResource(
            Res.string.reward_tokens,
            loginViewModel.getInitialBalanceReward(),
        )

    Box(modifier = modifier) {
        Background()
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentAlignment = Center,
        ) {
            SignupView(
                pageName = SignupPageName.SPLASH,
                openTerms = { termsLinkState = loginViewModel.getTncLink() },
                headlineText = getAnnotatedHeaderForLogin(fullText, rewardText),
                disclaimerText = "",
                onNavigateToCountrySelector = component::onNavigateToCountrySelector,
                onNavigateToOtpVerification = component::onNavigateToOtpVerification,
            )
        }
        if (termsLinkState.isNotEmpty()) {
            YralWebViewBottomSheet(
                link = termsLinkState,
                bottomSheetState = termsSheetState,
                onDismissRequest = { termsLinkState = "" },
            )
        }
    }
}

@Composable
private fun Background() {
    val isUnFolded = rememberScreenFoldStateProvider().isScreenUnfoldedFlow.collectAsState(false)
    val bottomImageHeight = if (isUnFolded.value) 300.dp else 250.dp
    val bottomImageAlignment = if (isUnFolded.value) BottomCenter else Center
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.login_top_shadow),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .height(150.dp)
                    .fillMaxWidth(),
        )
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Image(
                painter = painterResource(Res.drawable.login_bottom_shadow),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .height(bottomImageHeight)
                        .fillMaxWidth(),
            )
            Image(
                painter = painterResource(Res.drawable.login_coins),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = bottomImageAlignment,
                modifier =
                    Modifier
                        .height(bottomImageHeight)
                        .fillMaxWidth(),
            )
        }
    }
}
