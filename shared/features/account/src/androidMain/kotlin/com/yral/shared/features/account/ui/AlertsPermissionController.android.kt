package com.yral.shared.features.account.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

@Suppress("LongMethod")
@Composable
actual fun rememberAlertsPermissionController(viewModel: AccountsViewModel): AlertsPermissionController {
    val permissionsFactory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember(permissionsFactory) { permissionsFactory.createPermissionsController() }
    BindEffect(permissionsController)
    val context = LocalContext.current
    return remember(
        permissionsController,
        viewModel,
        context,
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
                                openNotificationSettings(context)
                                false
                            }
                        } catch (deniedAlways: DeniedAlwaysException) {
                            Logger.e("AccountScreen") {
                                "Notification permission permanently denied: ${deniedAlways.message}"
                            }
                            openNotificationSettings(context)
                            false
                        } catch (denied: DeniedException) {
                            Logger.e("AccountScreen") {
                                "Notification permission denied: ${denied.message}"
                            }
                            openNotificationSettings(context)
                            false
                        }
                    }
                    PermissionState.DeniedAlways -> {
                        openNotificationSettings(context)
                        false
                    }
                }
            } else {
                val currentlyGranted = permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                openNotificationSettings(context)
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

private suspend fun registerNotificationToken(viewModel: AccountsViewModel): Boolean {
    val tokenResult = runCatching { Firebase.messaging.getToken() }
    val token = tokenResult.getOrNull()
    if (!tokenResult.isSuccess || token.isNullOrBlank()) {
        Logger.e("AccountScreen") {
            "Failed to fetch FCM token for registration: ${tokenResult.exceptionOrNull()?.message}"
        }
        return false
    }
    return viewModel.registerAlerts(token)
}

private suspend fun deregisterNotificationToken(viewModel: AccountsViewModel): Boolean {
    val tokenResult = runCatching { Firebase.messaging.getToken() }
    val token = tokenResult.getOrNull()
    if (!tokenResult.isSuccess || token.isNullOrBlank()) {
        Logger.e("AccountScreen") {
            "Failed to fetch FCM token for deregistration: ${tokenResult.exceptionOrNull()?.message}"
        }
        return false
    }
    return viewModel.deregisterAlerts(token)
}

private fun openNotificationSettings(context: Context) {
    val primaryIntent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val intent =
        if (primaryIntent.resolveActivity(context.packageManager) != null) {
            primaryIntent
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    context.startActivity(intent)
}
