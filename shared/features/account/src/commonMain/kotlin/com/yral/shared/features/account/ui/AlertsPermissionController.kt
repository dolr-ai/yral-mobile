package com.yral.shared.features.account.ui

import androidx.compose.runtime.Composable
import com.yral.shared.features.account.viewmodel.AccountsViewModel

data class AlertsPermissionController(
    val toggle: suspend (Boolean) -> Boolean,
    val currentStatus: suspend () -> Boolean,
)

@Composable
expect fun rememberAlertsPermissionController(viewModel: AccountsViewModel): AlertsPermissionController
