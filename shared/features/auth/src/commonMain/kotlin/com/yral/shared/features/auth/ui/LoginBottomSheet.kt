package com.yral.shared.features.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.ui.LoginBottomSheetConstants.BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login_desc
import yral_mobile.shared.libs.designsystem.generated.resources.ok
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginBottomSheet(
    pageName: SignupPageName = SignupPageName.MENU,
    termsLink: String,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    onLoginSuccess: (() -> Unit)? = null,
    openTerms: () -> Unit,
    loginViewModel: LoginViewModel = koinViewModel(),
) {
    val dismissRequest = remember(onDismissRequest) { createDismissCallback(onDismissRequest, loginViewModel) }
    val loginSuccess =
        remember(onLoginSuccess) {
            onLoginSuccess?.let { createDismissCallback(it, loginViewModel) }
        }
    val context = getContext()
    val state = loginViewModel.state.collectAsStateWithLifecycle()
    when (state.value) {
        is UiState.Initial, is UiState.InProgress -> {
            YralBottomSheet(
                onDismissRequest = dismissRequest,
                bottomSheetState = bottomSheetState,
            ) {
                BoxWithConstraints {
                    val maxHeight = maxHeight
                    val adaptiveHeight = (maxHeight * BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN)
                    Column(
                        modifier =
                            Modifier
                                .padding(start = 16.dp, top = 45.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SignupView(
                            pageName = pageName,
                            onSignupClicked = { provider -> loginViewModel.signInWithSocial(context, provider) },
                            termsLink = termsLink,
                            openTerms = openTerms,
                        )
                        Spacer(modifier = Modifier.height(adaptiveHeight))
                    }
                }
            }
        }
        is UiState.Success<*> -> {
            LaunchedEffect(Unit) { loginSuccess?.invoke() ?: dismissRequest() }
        }
        is UiState.Failure -> {
            YralErrorMessage(
                title = stringResource(DesignRes.string.could_not_login),
                error = stringResource(DesignRes.string.could_not_login_desc),
                sheetState = bottomSheetState,
                cta = stringResource(DesignRes.string.ok),
                onClick = dismissRequest,
                onDismiss = dismissRequest,
            )
        }
    }
}

@Composable
internal expect fun getContext(): Any

private fun createDismissCallback(
    onDismiss: () -> Unit,
    loginViewModel: LoginViewModel,
): () -> Unit =
    {
        onDismiss()
        loginViewModel.sheetDismissed()
    }

private object LoginBottomSheetConstants {
    const val BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN = 0.3f
}
