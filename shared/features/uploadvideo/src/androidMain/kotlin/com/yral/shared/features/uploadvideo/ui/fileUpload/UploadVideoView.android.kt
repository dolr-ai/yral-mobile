package com.yral.shared.features.uploadvideo.ui.fileUpload

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.yral.shared.features.uploadvideo.utils.VideoFileManager
import com.yral.shared.features.uploadvideo.utils.VideoMetadataExtractor
import com.yral.shared.features.uploadvideo.utils.VideoPermissionUtils
import com.yral.shared.features.uploadvideo.utils.VideoValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Suppress("LongMethod")
@Composable
internal actual fun SelectVideoView(
    maxSeconds: Int,
    onVideoSelected: (String) -> Unit,
    onCTAClicked: () -> Unit,
) {
    var shouldLaunchPicker by remember { mutableStateOf(false) }
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    val permissionState =
        VideoPermissionUtils.rememberVideoPermissionsState { hasRequestedPermissions = true }

    var pickerError by remember { mutableStateOf<VideoPickerError?>(null) }
    var isProcessingVideo by remember { mutableStateOf(false) }

    val videoPickerLauncher =
        rememberVideoPickerLauncher(
            onVideoSelected = onVideoSelected,
            onProcessingStateChange = { isProcessingVideo = it },
            onError = { error -> pickerError = error },
        )

    VideoSelectionPermissionHandler(
        permissionState = permissionState,
        shouldLaunchPicker = shouldLaunchPicker,
        hasRequestedPermissions = hasRequestedPermissions,
        onShouldLaunchPickerChange = { shouldLaunchPicker = it },
        onHasRequestedPermissionsChange = { hasRequestedPermissions = it },
        videoPickerLauncher = videoPickerLauncher,
    )

    VideoSelectionContent(
        maxSeconds = maxSeconds,
        isProcessingVideo = isProcessingVideo,
        onSelectFileClick = {
            if (!isProcessingVideo) {
                onCTAClicked()
                val hasPermissions =
                    VideoPermissionUtils.hasSufficientVideoPermissions(permissionState)
                if (hasPermissions) {
                    videoPickerLauncher.launch("video/*")
                } else {
                    shouldLaunchPicker = true
                    permissionState.launchMultiplePermissionRequest()
                }
            }
        },
    )

    val pickerErrorValue = pickerError

    if (pickerErrorValue != null) {
        VideoSelectionPickerErrorDialog(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            pickerError = pickerErrorValue,
            onDismissError = {
                pickerError = null
            },
        )
    }
}

@Composable
private fun rememberVideoPickerLauncher(
    onVideoSelected: (String) -> Unit,
    onProcessingStateChange: (Boolean) -> Unit,
    onError: (VideoPickerError) -> Unit,
): ActivityResultLauncher<String> {
    val context = LocalContext.current
    val videoValidator: VideoValidator = koinInject()
    val videoFileManager: VideoFileManager = koinInject()
    val coroutineScope = rememberCoroutineScope()

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { contentUri ->
            onProcessingStateChange(true)
            coroutineScope.launch {
                // Validate video before copying
                val validationResult =
                    withContext(Dispatchers.IO) {
                        videoValidator.validateVideoFromUri(context, contentUri)
                    }

                validationResult
                    .onSuccess { validationSuccess ->
                        // Video is valid, proceed with copying
                        val fileName = "selected_video_${System.currentTimeMillis()}.mp4"
                        val result =
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    videoFileManager.copyVideoFromUri(context, contentUri, fileName)
                                }
                            }
                        result
                            .onSuccess { filePath ->
                                if (filePath != null) {
                                    onVideoSelected(filePath)
                                } else {
                                    Logger.e("Failed to copy video from URI: $contentUri")
                                    onError(VideoPickerError.ProcessingFailed)
                                }
                            }.onFailure { exception ->
                                Logger.e("Error processing video", exception)
                                onError(VideoPickerError.ProcessingFailed)
                            }
                    }.onFailure { validationError ->
                        // Video validation failed, show error
                        onError(VideoPickerError.Validation(validationError))
                    }
                onProcessingStateChange(false)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun VideoSelectionPermissionHandler(
    permissionState: MultiplePermissionsState,
    shouldLaunchPicker: Boolean,
    hasRequestedPermissions: Boolean,
    onShouldLaunchPickerChange: (Boolean) -> Unit,
    onHasRequestedPermissionsChange: (Boolean) -> Unit,
    videoPickerLauncher: ActivityResultLauncher<String>,
) {
    var showPermissionError by remember { mutableStateOf(false) }

    // Launch video picker when permissions are sufficient and flag is set
    LaunchedEffect(
        VideoPermissionUtils.hasSufficientVideoPermissions(permissionState),
        shouldLaunchPicker,
    ) {
        if (VideoPermissionUtils.hasSufficientVideoPermissions(permissionState) && shouldLaunchPicker) {
            onShouldLaunchPickerChange(false)
            videoPickerLauncher.launch("video/*")
        }
    }

    val grantedCount =
        remember {
            derivedStateOf {
                permissionState.permissions.count {
                    it.status.isGranted
                }
            }
        }
    val noRationaleDeniedCount =
        remember {
            derivedStateOf {
                permissionState.permissions.count {
                    !it.status.isGranted && !it.status.shouldShowRationale
                }
            }
        }
    LaunchedEffect(
        hasRequestedPermissions,
        grantedCount.value,
        noRationaleDeniedCount.value,
    ) {
        if (hasRequestedPermissions &&
            !VideoPermissionUtils.hasSufficientVideoPermissions(permissionState) &&
            VideoPermissionUtils.arePermissionsPermanentlyDenied(permissionState)
        ) {
            onShouldLaunchPickerChange(false)
            onHasRequestedPermissionsChange(false)
            showPermissionError = true
        }
    }

    if (showPermissionError) {
        val context = LocalContext.current
        VideoSelectionPermissionErrorDialog(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismissError = {
                showPermissionError = false
            },
            onGoToSettingsClicked = {
                // Open device settings
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                context.startActivity(intent)
            },
        )
    }
}

@Composable
actual fun formatFileSize(
    bytes: Long,
    precision: Int,
): String {
    val videoMetadataExtractor: VideoMetadataExtractor = koinInject()
    return videoMetadataExtractor.formatFileSize(bytes, precision)
}

actual fun formatMaxDuration(duration: Double): String = "%.0f".format(duration)
