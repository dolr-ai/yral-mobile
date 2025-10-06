package com.yral.android.ui.screens.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessaging
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.tasks.await
import org.koin.compose.koinInject

@Composable
fun rememberAlertsToggleHandler(): suspend (Boolean) -> Boolean {
    val permissionsFactory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember(permissionsFactory) { permissionsFactory.createPermissionsController() }
    BindEffect(permissionsController)
    val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase = koinInject()
    val deregisterNotificationTokenUseCase: DeregisterNotificationTokenUseCase = koinInject()
    return remember(permissionsController, registerNotificationTokenUseCase, deregisterNotificationTokenUseCase) {
        val handler: suspend (Boolean) -> Boolean = { enabled ->
            if (enabled) {
                permissionsController.getPermissionState(Permission.REMOTE_NOTIFICATION)
                try {
                    permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
                    val granted = permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)
                    if (granted) {
                        registerNotificationToken(registerNotificationTokenUseCase)
                    } else {
                        false
                    }
                } catch (deniedAlways: DeniedAlwaysException) {
                    Logger.e("AccountScreen") {
                        "Notification permission permanently denied: ${deniedAlways.message}"
                    }
                    false
                } catch (denied: DeniedException) {
                    Logger.e("AccountScreen") {
                        "Notification permission denied: ${denied.message}"
                    }
                    false
                }
            } else {
                val deregistered = deregisterNotificationToken(deregisterNotificationTokenUseCase)
                if (!deregistered) {
                    Logger.e("AccountScreen") { "Failed to deregister notifications" }
                }
                false
            }
        }
        handler
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
