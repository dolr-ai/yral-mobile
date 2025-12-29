package com.yral.shared.features.chat.ui.conversation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.attachments.persistUriToChatCache
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
actual fun rememberChatImagePicker(onImagePicked: (FilePathChatAttachment) -> Unit): () -> Unit {
    val context = LocalContext.current

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            runCatching {
                persistUriToChatCache(
                    context = context,
                    uri = uri,
                )
            }.onSuccess { attachment ->
                onImagePicked(attachment)
            }.onFailure { throwable ->
                Logger.e(throwable) { "Failed to persist selected image to cache" }
            }
        }

    return remember(galleryLauncher) {
        { galleryLauncher.launch("image/*") }
    }
}

@OptIn(ExperimentalTime::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
actual fun rememberChatImageCapture(onImagePicked: (FilePathChatAttachment) -> Unit): () -> Unit {
    val context = LocalContext.current
    val tempFileState = remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()
    var pendingCapture by remember { mutableStateOf(false) }

    // Setup moko-permissions controller
    val permissionsFactory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember(permissionsFactory) { permissionsFactory.createPermissionsController() }
    BindEffect(permissionsController)

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { success: Boolean ->
            val file = tempFileState.value
            if (success && file != null) {
                runCatching {
                    persistUriToChatCache(
                        context = context,
                        uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
                    )
                }.onSuccess { attachment ->
                    onImagePicked(attachment)
                }.onFailure { throwable ->
                    Logger.e(throwable) { "Failed to persist captured image to cache" }
                }
            }
            if (file != null && file.exists()) {
                if (!file.delete()) {
                    Logger.w { "Unable to delete temp image file" }
                }
            }
            tempFileState.value = null
        }

    val startCapture: () -> Unit =
        remember(cameraLauncher, context) {
            {
                val imageFile =
                    runCatching {
                        File.createTempFile(
                            "chat_image_${Clock.System.now().toEpochMilliseconds()}",
                            ".jpg",
                            context.cacheDir,
                        )
                    }.onFailure { error ->
                        Logger.e(error) { "Failed to create temp file for image capture" }
                    }.getOrNull()

                if (imageFile == null) {
                    return@remember
                }
                tempFileState.value = imageFile
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, imageFile)
                cameraLauncher.launch(uri)
            }
        }

    // Handle pending capture after permission is granted
    LaunchedEffect(pendingCapture) {
        if (pendingCapture) {
            val isGranted = permissionsController.isPermissionGranted(Permission.CAMERA)
            if (isGranted) {
                pendingCapture = false
                startCapture()
            }
        }
    }

    return remember(permissionsController) {
        {
            scope.launch {
                when (permissionsController.getPermissionState(Permission.CAMERA)) {
                    PermissionState.Granted -> {
                        startCapture()
                    }
                    PermissionState.Denied,
                    PermissionState.NotDetermined,
                    PermissionState.NotGranted,
                    -> {
                        try {
                            permissionsController.providePermission(Permission.CAMERA)
                            if (permissionsController.isPermissionGranted(Permission.CAMERA)) {
                                startCapture()
                            } else {
                                pendingCapture = true
                            }
                        } catch (deniedAlways: DeniedAlwaysException) {
                            Logger.e("ChatImagePicker") {
                                "Camera permission permanently denied: ${deniedAlways.message}"
                            }
                            permissionsController.openAppSettings()
                        } catch (denied: DeniedException) {
                            Logger.e("ChatImagePicker") {
                                "Camera permission denied: ${denied.message}"
                            }
                        }
                    }
                    PermissionState.DeniedAlways -> {
                        permissionsController.openAppSettings()
                    }
                }
            }
        }
    }
}
