package com.yral.shared.features.auth.ui

import androidx.compose.runtime.Composable
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.phonevalidation.countries.Country

interface LoginCoordinator {
    val loginViewModel: LoginViewModel

    fun requestLogin(loginInfo: LoginInfo): @Composable () -> Unit

    fun navigateToCountrySelector(onCountrySelected: (Country) -> Unit)

    fun navigateToOtpVerification()

    fun dismissLoginBottomSheet()
}

typealias RequestLoginFactory = @Composable (LoginInfo) -> @Composable () -> Unit

fun LoginCoordinator.toRequestFactory(): RequestLoginFactory = { loginInfo -> this.requestLogin(loginInfo) }
