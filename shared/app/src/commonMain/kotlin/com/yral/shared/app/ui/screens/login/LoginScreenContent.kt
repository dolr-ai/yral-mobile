package com.yral.shared.app.ui.screens.login

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.ui.screens.MandatoryLoginScreen
import com.yral.shared.features.auth.ui.CountrySelectorScreen
import com.yral.shared.features.auth.ui.OtpVerificationScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    child: RootComponent.Child,
    rootComponent: RootComponent,
    modifier: Modifier,
) {
    val state by rootComponent.loginViewModel.state.collectAsState()
    val stack by rootComponent.stack.subscribeAsState()
    LaunchedEffect(state) {
        if (state.isLoginComplete()) {
            rootComponent.currentLoginInfo?.onSuccess?.invoke()
            rootComponent.clearLoginState()
            rootComponent.loginViewModel.resetState()
            if (child is RootComponent.Child.OtpVerification &&
                !stack.backStack.any { it.instance is RootComponent.Child.MandatoryLogin }
            ) {
                child.component.onBack()
            }
        }
    }

    when (child) {
        is RootComponent.Child.CountrySelector -> {
            CountrySelectorScreen(
                component = child.component,
                loginViewModel = rootComponent.loginViewModel,
                modifier = modifier,
            )
        }

        is RootComponent.Child.OtpVerification -> {
            OtpVerificationScreen(
                component = child.component,
                loginViewModel = rootComponent.loginViewModel,
                modifier = modifier,
            )
        }

        is RootComponent.Child.MandatoryLogin -> {
            MandatoryLoginScreen(
                modifier = modifier,
                component = child.component,
                loginViewModel = rootComponent.loginViewModel,
            )
        }

        else -> Unit
    }
}
