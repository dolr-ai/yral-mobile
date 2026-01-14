package com.yral.shared.app.ui.screens.login

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.features.auth.ui.LoginBottomSheet
import com.yral.shared.features.auth.ui.LoginScreenType
import com.yral.shared.libs.phonevalidation.countries.Country

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginBottomSheetSlotContent(rootComponent: RootComponent) {
    val loginInfo = rootComponent.currentLoginInfo ?: return
    val screenType = loginInfo.screenType as? LoginScreenType.BottomSheet ?: return
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val state by rootComponent.loginViewModel.state.collectAsState()
    LaunchedEffect(state) {
        if (state.isLoginComplete()) {
            loginInfo.onSuccess?.invoke()
            rootComponent.clearLoginState()
            rootComponent.getLoginCoordinator().dismissLoginBottomSheet()
        }
    }

    val loginCoordinator = rootComponent.getLoginCoordinator()
    LoginBottomSheet(
        pageName = loginInfo.pageName,
        tncLink = rootComponent.loginViewModel.getTncLink(),
        initialBalanceReward = rootComponent.loginViewModel.getInitialBalanceReward(),
        bottomSheetState = bottomSheetState,
        onDismissRequest = { loginCoordinator.dismissLoginBottomSheet() },
        onNavigateToOtpVerification = { loginCoordinator.navigateToOtpVerification() },
        onNavigateToCountrySelector = {
            loginCoordinator.navigateToCountrySelector { country: Country ->
                rootComponent.loginViewModel.onCountrySelected(country)
            }
        },
        bottomSheetType = screenType.bottomSheetType,
        mode = loginInfo.mode,
    )
}
