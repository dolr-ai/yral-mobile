package com.yral.android.ui.screens.account

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessaging
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.tasks.await
import org.koin.compose.koinInject

data class AlertsPermissionController(
    val toggle: suspend (Boolean) -> Boolean,
    val currentStatus: suspend () -> Boolean,
)

@Suppress("LongMethod")
@Composable
fun rememberAlertsPermissionController(): AlertsPermissionController {
    val permissionsFactory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember(permissionsFactory) { permissionsFactory.createPermissionsController() }
    BindEffect(permissionsController)
    val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase = koinInject()
    val deregisterNotificationTokenUseCase: DeregisterNotificationTokenUseCase = koinInject()
    val context = LocalContext.current
    return remember(
        permissionsController,
        registerNotificationTokenUseCase,
        deregisterNotificationTokenUseCase,
        context,
    ) {
        val toggle: suspend (Boolean) -> Boolean = { enabled ->
            if (enabled) {
                when (permissionsController.getPermissionState(Permission.REMOTE_NOTIFICATION)) {
                    PermissionState.Granted -> registerNotificationToken(registerNotificationTokenUseCase)
                    PermissionState.Denied,
                    PermissionState.NotDetermined,
                    PermissionState.NotGranted,
                    -> {
                        try {
                            permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
                            if (permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)) {
                                registerNotificationToken(registerNotificationTokenUseCase)
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
                    val deregistered = deregisterNotificationToken(deregisterNotificationTokenUseCase)
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
                val deregistered = deregisterNotificationToken(deregisterNotificationTokenUseCase)
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

private suspend fun registerNotificationToken(registerUseCase: RegisterNotificationTokenUseCase): Boolean {
    val tokenResult = runCatching { FirebaseMessaging.getInstance().token.await() }
    val token = tokenResult.getOrNull()
    if (!tokenResult.isSuccess || token.isNullOrBlank()) {
        Logger.e("AccountScreen") {
            "Failed to fetch FCM token for registration: ${tokenResult.exceptionOrNull()?.message}"
        }
        return false
    }
    return runCatching {
        registerUseCase(RegisterNotificationTokenUseCase.Parameter(token = token))
    }.onFailure { error ->
        Logger.e("AccountScreen") {
            "Failed to register notifications: ${error.message}"
        }
    }.isSuccess
}

private suspend fun deregisterNotificationToken(deregisterUseCase: DeregisterNotificationTokenUseCase): Boolean {
    val tokenResult = runCatching { FirebaseMessaging.getInstance().token.await() }
    val token = tokenResult.getOrNull()
    if (!tokenResult.isSuccess || token.isNullOrBlank()) {
        Logger.e("AccountScreen") {
            "Failed to fetch FCM token for deregistration: ${tokenResult.exceptionOrNull()?.message}"
        }
        return false
    }
    return runCatching {
        deregisterUseCase(DeregisterNotificationTokenUseCase.Parameter(token = token))
    }.onFailure { error ->
        Logger.e("AccountScreen") {
            "Failed to deregister notifications: ${error.message}"
        }
    }.isSuccess
}

private fun openNotificationSettings(context: android.content.Context) {
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
