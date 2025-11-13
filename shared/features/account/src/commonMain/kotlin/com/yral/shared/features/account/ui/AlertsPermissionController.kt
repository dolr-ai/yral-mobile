package com.yral.shared.features.account.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

data class AlertsPermissionController(
    val toggle: suspend (Boolean) -> Boolean,
    val currentStatus: suspend () -> Boolean,
)

@Suppress("LongMethod")
@Composable
fun rememberAlertsPermissionController(viewModel: AccountsViewModel): AlertsPermissionController {
    val permissionsFactory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember(permissionsFactory) { permissionsFactory.createPermissionsController() }
    BindEffect(permissionsController)
    return remember(
        permissionsController,
        viewModel,
    ) {
        val toggle: suspend (Boolean) -> Boolean = { enabled ->
            if (enabled) {
                when (permissionsController.getPermissionState(Permission.REMOTE_NOTIFICATION)) {
                    PermissionState.Granted -> registerNotificationToken(viewModel)
                    PermissionState.Denied,
                    PermissionState.NotDetermined,
                    PermissionState.NotGranted,
                    -> {
                        try {
                            permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
                            if (permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)) {
                                registerNotificationToken(viewModel)
                            } else {
                                permissionsController.openAppSettings()
                                false
                            }
                        } catch (deniedAlways: DeniedAlwaysException) {
                            Logger.e("AccountScreen") {
                                "Notification permission permanently denied: ${deniedAlways.message}"
                            }
                            permissionsController.openAppSettings()
                            false
                        } catch (denied: DeniedException) {
                            Logger.e("AccountScreen") {
                                "Notification permission denied: ${denied.message}"
                            }
                            permissionsController.openAppSettings()
                            false
                        }
                    }
                    PermissionState.DeniedAlways -> {
                        permissionsController.openAppSettings()
                        false
                    }
                }
            } else {
                val currentlyGranted = permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                permissionsController.openAppSettings()
                if (!currentlyGranted) {
                    val deregistered = deregisterNotificationToken(viewModel)
                    if (!deregistered) {
                        Logger.e("AccountScreen") { "Failed to deregister notifications" }
                    }
                }
                currentlyGranted
            }
        }
        val status: suspend () -> Boolean = {
            val granted = permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
            if (!granted) {
                val deregistered = deregisterNotificationToken(viewModel)
                if (!deregistered) {
                    Logger.e("AccountScreen") {
                        "Failed to deregister notifications after permission revocation"
                    }
                }
            }
            granted
        }
        AlertsPermissionController(toggle = toggle, currentStatus = status)
    }
}

private suspend fun registerNotificationToken(viewModel: AccountsViewModel): Boolean = viewModel.registerAlerts()

private suspend fun deregisterNotificationToken(viewModel: AccountsViewModel): Boolean = viewModel.deregisterAlerts()
