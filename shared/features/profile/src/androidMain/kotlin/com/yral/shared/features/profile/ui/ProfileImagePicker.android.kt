package com.yral.shared.features.profile.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import co.touchlab.kermit.Logger
import java.io.File

@Composable
actual fun rememberProfileImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                }
            }.onSuccess { bytes ->
                if (bytes != null) {
                    onImagePicked(bytes)
                }
            }.onFailure { throwable ->
                Logger.e(throwable) { "Failed to read selected profile image" }
            }
        }

    return remember(pickerLauncher) {
        { pickerLauncher.launch("image/*") }
    }
}

@Suppress("LongMethod")
@Composable
actual fun rememberProfilePhotoCapture(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val tempFileState = remember { mutableStateOf<File?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { success: Boolean ->
            val file = tempFileState.value
            if (success && file != null) {
                runCatching {
                    file.inputStream().use { it.readBytes() }
                }.onSuccess { bytes ->
                    onImagePicked(bytes)
                }.onFailure { throwable ->
                    Logger.e(throwable) { "Failed to capture profile image" }
                }
            }
            if (file != null && file.exists()) {
                if (!file.delete()) {
                    Logger.w { "Unable to delete temp profile image file" }
                }
            }
            tempFileState.value = null
        }

    val startCapture =
        remember(cameraLauncher, context) {
            {
                val imageFile =
                    runCatching {
                        File.createTempFile("profile_photo_", ".jpg", context.cacheDir)
                    }.onFailure { error ->
                        Logger.e(error) { "Failed to create temp file for profile photo" }
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

    val pendingCapture = remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && pendingCapture.value) {
                pendingCapture.value = false
                startCapture()
            } else {
                pendingCapture.value = false
            }
        }

    return remember(cameraLauncher, permissionLauncher) {
        {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCapture()
            } else {
                pendingCapture.value = true
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
