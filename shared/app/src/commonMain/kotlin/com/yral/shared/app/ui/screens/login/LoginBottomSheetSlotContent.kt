package com.yral.shared.app.ui.screens.login

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import co.touchlab.kermit.Logger
import com.yral.shared.analytics.events.SignupNudgeDismissAction
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.ui.LoginBottomSheet
import com.yral.shared.features.auth.ui.LoginScreenType
import com.yral.shared.libs.phonevalidation.countries.Country
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginBottomSheetSlotContent(
    rootComponent: RootComponent,
    authTelemetry: AuthTelemetry = koinInject(),
) {
    val loginInfo = rootComponent.currentLoginInfo
    LaunchedEffect(loginInfo) {
        if (loginInfo == null) {
            Logger.d("LoginBottomSheetSlotContent") { "LoginInfo not available for slot" }
            rootComponent.getLoginCoordinator().dismissLoginBottomSheet()
            return@LaunchedEffect
        }
    }

    // Early return if LoginInfo is not available
    val validLoginInfo = loginInfo ?: return
    val screenType = validLoginInfo.screenType as? LoginScreenType.BottomSheet ?: return
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val state by rootComponent.loginViewModel.state.collectAsState()
    LaunchedEffect(state) {
        if (state.isLoginComplete()) {
            validLoginInfo.onSuccess?.invoke()
            rootComponent.clearLoginState()
            rootComponent.getLoginCoordinator().dismissLoginBottomSheet()
        }
    }

    val loginCoordinator = rootComponent.getLoginCoordinator()
    LoginBottomSheet(
        pageName = validLoginInfo.pageName,
        tncLink = rootComponent.loginViewModel.getTncLink(),
        initialBalanceReward = rootComponent.loginViewModel.getInitialBalanceReward(),
        bottomSheetState = bottomSheetState,
        onDismissRequest = {
            // User-initiated dismissal (tap outside / swipe down)
            authTelemetry.onSignupNudgeDismissed(SignupNudgeDismissAction.CLOSE)
            loginCoordinator.dismissLoginBottomSheet()
        },
        onNavigateToOtpVerification = { loginCoordinator.navigateToOtpVerification() },
        onNavigateToCountrySelector = {
            loginCoordinator.navigateToCountrySelector { country: Country ->
                rootComponent.loginViewModel.onCountrySelected(country)
            }
        },
        bottomSheetType = screenType.bottomSheetType,
        mode = validLoginInfo.mode,
    )
}
