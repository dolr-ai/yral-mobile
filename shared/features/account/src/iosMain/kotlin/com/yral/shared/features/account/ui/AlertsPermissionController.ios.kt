package com.yral.shared.features.account.ui

import androidx.compose.runtime.Composable
import com.yral.shared.features.account.viewmodel.AccountsViewModel

@Composable
actual fun rememberAlertsPermissionController(viewModel: AccountsViewModel): AlertsPermissionController =
    AlertsPermissionController(
        toggle = {
            // Remote notifications are handled natively on iOS for now.
            // Return the previous state to avoid toggling inside shared UI.
            viewModel.state.value.alertsEnabled
        },
        currentStatus = {
            viewModel.state.value.alertsEnabled
        },
    )
