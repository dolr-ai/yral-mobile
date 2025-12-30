package com.yral.shared.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.ui.SignupView
import com.yral.shared.features.auth.ui.getAnnotatedHeaderForLogin
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.login_to_unlock_tokens
import yral_mobile.shared.app.generated.resources.login_top_shadow
import yral_mobile.shared.app.generated.resources.reward_tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryLoginScreen(modifier: Modifier) {
    val loginViewModel: LoginViewModel = koinViewModel()
    val termsLink = loginViewModel.getTncLink()
    val context = getContext()
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

    Box(modifier = modifier.padding(horizontal = 16.dp)) {
        Image(
            painter = painterResource(Res.drawable.login_top_shadow),
            modifier = Modifier.align(Alignment.TopCenter),
            contentDescription = "",
            contentScale = ContentScale.None,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Center,
        ) {
            SignupView(
                pageName = SignupPageName.SPLASH,
                termsLink = termsLink,
                onSignupClicked = { provider ->
                    loginViewModel.signInWithSocial(
                        context,
                        provider,
                    )
                },
                openTerms = { termsLinkState = termsLink },
                headlineText = getAnnotatedHeaderForLogin(fullText, rewardText),
                disclaimerText = "",
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
internal expect fun getContext(): Any
