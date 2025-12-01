package com.yral.shared.features.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.storage.WRITE_STORAGE

data class StoragePermissionController(
    val requestPermission: suspend () -> Boolean,
    val isPermissionGranted: suspend () -> Boolean,
)

@Composable
fun rememberStoragePermissionController(): StoragePermissionController {
    val permissionsFactory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember(permissionsFactory) { permissionsFactory.createPermissionsController() }
    BindEffect(permissionsController)

    return remember(permissionsController) {
        val requestPermission: suspend () -> Boolean = {
            // Check if permission is actually required for this platform/version
            if (!isStoragePermissionRequired()) {
                // On Android Q+, MediaStore API doesn't require permission
                // Return true to proceed with download
                true
            } else {
                // Permission is required, proceed with normal permission flow
                when (permissionsController.getPermissionState(Permission.WRITE_STORAGE)) {
                    PermissionState.Granted -> true
                    PermissionState.Denied,
                    PermissionState.NotDetermined,
                    PermissionState.NotGranted,
                    -> {
                        try {
                            permissionsController.providePermission(Permission.WRITE_STORAGE)
                            permissionsController.isPermissionGranted(Permission.WRITE_STORAGE)
                        } catch (deniedAlways: DeniedAlwaysException) {
                            Logger.e("ProfileScreen") {
                                "Storage permission permanently denied: ${deniedAlways.message}"
                            }
                            permissionsController.openAppSettings()
                            false
                        } catch (denied: DeniedException) {
                            Logger.e("ProfileScreen") {
                                "Storage permission denied: ${denied.message}"
                            }
                            false
                        }
                    }
                    PermissionState.DeniedAlways -> {
                        permissionsController.openAppSettings()
                        false
                    }
                }
            }
        }

        val isPermissionGranted: suspend () -> Boolean = {
            // If permission is not required, always return true
            if (!isStoragePermissionRequired()) {
                true
            } else {
                permissionsController.isPermissionGranted(Permission.WRITE_STORAGE)
            }
        }

        StoragePermissionController(
            requestPermission = requestPermission,
            isPermissionGranted = isPermissionGranted,
        )
    }
}
