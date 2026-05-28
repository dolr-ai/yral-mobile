package com.yral.shared.features.uploadvideo.ui.aiVideoGen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger

@Composable
actual fun rememberAiVideoImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit {
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
                Logger.e(throwable) { "Failed to read selected AI video image" }
            }
        }

    return remember(pickerLauncher) {
        { pickerLauncher.launch("image/*") }
    }
}
