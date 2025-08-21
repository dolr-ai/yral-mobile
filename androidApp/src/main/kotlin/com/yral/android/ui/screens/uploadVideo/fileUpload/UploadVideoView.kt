package com.yral.android.ui.screens.uploadVideo.fileUpload

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralButton
import com.yral.android.ui.widgets.YralButtonState
import com.yral.android.ui.widgets.YralErrorMessage
import com.yral.android.ui.widgets.video.VideoFileManager
import com.yral.android.ui.widgets.video.VideoMetadataExtractor
import com.yral.android.ui.widgets.video.VideoPermissionUtils
import com.yral.android.ui.widgets.video.VideoValidator
import com.yral.android.ui.widgets.video.YralVideoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

const val UPLOAD_BOX_ASPECT_RATIO = 0.75f

@Composable
fun UploadVideo(
    videoFilePath: String,
    onVideoSelected: (String) -> Unit,
    onCTAClicked: () -> Unit,
) {
    val videoFileManager: VideoFileManager = koinInject()
    val coroutineScope = rememberCoroutineScope()
    Row(Modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier =
                Modifier
                    .border(
                        width = 1.dp,
                        color = YralColors.Neutral800,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).fillMaxWidth()
                    .aspectRatio(UPLOAD_BOX_ASPECT_RATIO)
                    .padding(8.dp)
                    .background(
                        color = YralColors.Neutral950,
                        shape = RoundedCornerShape(size = 8.dp),
                    ),
        ) {
            if (videoFilePath.isNotEmpty()) {
                Box(Modifier.fillMaxSize()) {
                    YralVideoPlayer(
                        modifier = Modifier.fillMaxSize(),
                        url = videoFilePath,
                        autoPlay = true,
                        onError = { error ->
                            Logger.d("Video error: $error")
                        },
                    )
                    Image(
                        painter = painterResource(id = R.drawable.cross),
                        contentDescription = "Remove video",
                        contentScale = ContentScale.None,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .clickable { onVideoSelected("") },
                    )
                }
            } else {
                SelectVideoView(
                    onVideoSelected = onVideoSelected,
                    onCTAClicked = onCTAClicked,
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SelectVideoView(
    maxSeconds: Int = 60,
    onVideoSelected: (String) -> Unit,
    onCTAClicked: () -> Unit,
) {
    var shouldLaunchPicker by remember { mutableStateOf(false) }
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    val permissionState =
        VideoPermissionUtils.rememberVideoPermissionsState { hasRequestedPermissions = true }

    var showPermissionError by remember { mutableStateOf(false) }
    var pickerError by remember { mutableStateOf<VideoPickerError?>(null) }
    var isProcessingVideo by remember { mutableStateOf(false) }
    val errorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectionState =
        VideoSelectionState(
            shouldLaunchPicker = shouldLaunchPicker,
            hasRequestedPermissions = hasRequestedPermissions,
            showPermissionError = showPermissionError,
            isProcessingVideo = isProcessingVideo,
            errorSheetState = errorSheetState,
        )

    val videoPickerLauncher =
        rememberVideoPickerLauncher(
            onVideoSelected = onVideoSelected,
            onProcessingStateChange = { isProcessingVideo = it },
            onError = { error -> pickerError = error },
        )

    VideoSelectionPermissionHandler(
        permissionState = permissionState,
        selectionState = selectionState,
        onShouldLaunchPickerChange = { shouldLaunchPicker = it },
        onHasRequestedPermissionsChange = { hasRequestedPermissions = it },
        onShowPermissionErrorChange = { showPermissionError = it },
        videoPickerLauncher = videoPickerLauncher,
    )

    VideoSelectionContent(
        maxSeconds = maxSeconds,
        permissionState = permissionState,
        selectionState = selectionState,
        onLaunchVideoPicker = {
            onCTAClicked()
            videoPickerLauncher.launch("video/*")
        },
        onRequestPermissions = {
            onCTAClicked()
            shouldLaunchPicker = true
            permissionState.launchMultiplePermissionRequest()
        },
    )

    VideoSelectionErrorDialog(
        selectionState = selectionState,
        pickerError = pickerError,
        onDismissError = {
            showPermissionError = false
            pickerError = null
        },
    )
}

sealed class VideoPickerError {
    data class Validation(
        val error: VideoValidator.ValidationError,
    ) : VideoPickerError()
    object ProcessingFailed : VideoPickerError()
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
    selectionState: VideoSelectionState,
    onShouldLaunchPickerChange: (Boolean) -> Unit,
    onHasRequestedPermissionsChange: (Boolean) -> Unit,
    onShowPermissionErrorChange: (Boolean) -> Unit,
    videoPickerLauncher: ActivityResultLauncher<String>,
) {
    // Launch video picker when permissions are sufficient and flag is set
    LaunchedEffect(
        VideoPermissionUtils.hasSufficientVideoPermissions(permissionState),
        selectionState.shouldLaunchPicker,
    ) {
        if (VideoPermissionUtils.hasSufficientVideoPermissions(permissionState) && selectionState.shouldLaunchPicker) {
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
        selectionState.hasRequestedPermissions,
        grantedCount.value,
        noRationaleDeniedCount.value,
    ) {
        if (selectionState.hasRequestedPermissions &&
            !VideoPermissionUtils.hasSufficientVideoPermissions(permissionState) &&
            VideoPermissionUtils.arePermissionsPermanentlyDenied(permissionState)
        ) {
            onShouldLaunchPickerChange(false)
            onHasRequestedPermissionsChange(false)
            onShowPermissionErrorChange(true)
        }
    }

    // Handle error sheet visibility
    LaunchedEffect(selectionState.showPermissionError) {
        if (selectionState.showPermissionError) {
            selectionState.errorSheetState.show()
        } else {
            selectionState.errorSheetState.hide()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun VideoSelectionContent(
    maxSeconds: Int,
    permissionState: MultiplePermissionsState,
    selectionState: VideoSelectionState,
    onLaunchVideoPicker: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.upload_video_to_share_with_world),
            style = LocalAppTopography.current.mdMedium,
            color = YralColors.NeutralTextPrimary,
        )
        Text(
            text = stringResource(R.string.video_file_max_seconds, maxSeconds),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.NeutralTextSecondary,
        )
        Spacer(Modifier.height(0.dp))
        VideoSelectionButton(
            isProcessingVideo = selectionState.isProcessingVideo,
            hasPermissions = VideoPermissionUtils.hasSufficientVideoPermissions(permissionState),
            onLaunchVideoPicker = onLaunchVideoPicker,
            onRequestPermissions = onRequestPermissions,
        )
    }
}

@Composable
private fun VideoSelectionButton(
    isProcessingVideo: Boolean,
    hasPermissions: Boolean,
    onLaunchVideoPicker: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    YralButton(
        modifier =
            Modifier
                .widthIn(min = 107.dp),
        text =
            if (isProcessingVideo) {
                ""
            } else {
                stringResource(R.string.select_file)
            },
        borderWidth = 1.dp,
        borderColor = YralColors.Pink300,
        backgroundColor = YralColors.Neutral900,
        textStyle = TextStyle(color = YralColors.Pink300),
        buttonState = if (isProcessingVideo) YralButtonState.Loading else YralButtonState.Enabled,
    ) {
        if (!isProcessingVideo) {
            if (hasPermissions) {
                onLaunchVideoPicker()
            } else {
                onRequestPermissions()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoSelectionErrorDialog(
    selectionState: VideoSelectionState,
    pickerError: VideoPickerError?,
    onDismissError: () -> Unit,
) {
    if (selectionState.showPermissionError) {
        val context = LocalContext.current
        YralErrorMessage(
            title = stringResource(R.string.permission_required_title),
            error = stringResource(R.string.permission_required_description),
            sheetState = selectionState.errorSheetState,
            cta = stringResource(R.string.go_to_settings),
            onClick = {
                onDismissError()
                // Open device settings
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                context.startActivity(intent)
            },
            onDismiss = onDismissError,
        )
    }

    if (pickerError != null) {
        YralErrorMessage(
            title = stringResource(R.string.error),
            error = pickerError.toErrorMessage(),
            sheetState = selectionState.errorSheetState,
            cta = stringResource(R.string.ok),
            onClick = onDismissError,
            onDismiss = onDismissError,
        )
    }
}

@Composable
private fun VideoPickerError.toErrorMessage(): String {
    val videoMetadataExtractor: VideoMetadataExtractor = koinInject()

    return when (this) {
        is VideoPickerError.Validation ->
            when (val error = this.error) {
                is VideoValidator.ValidationError.UnableToReadDuration ->
                    stringResource(R.string.video_validation_unable_to_read_duration)
                is VideoValidator.ValidationError.UnableToReadFileSize ->
                    stringResource(R.string.video_validation_unable_to_read_file_size)
                is VideoValidator.ValidationError.DurationExceedsLimit ->
                    stringResource(
                        R.string.video_validation_duration_exceeds_limit_with_data,
                        "%.0f".format(error.limit),
                    )
                is VideoValidator.ValidationError.FileSizeExceedsLimit ->
                    stringResource(
                        R.string.video_validation_file_size_exceeds_limit_with_data,
                        videoMetadataExtractor.formatFileSize(error.limit, precision = 0),
                    )
            }

        is VideoPickerError.ProcessingFailed -> stringResource(R.string.video_validation_processing_failed)
    }
}

/**
 * Data class to hold all state variables for the video selection screen
 */
@OptIn(ExperimentalMaterial3Api::class)
data class VideoSelectionState(
    val shouldLaunchPicker: Boolean = false,
    val hasRequestedPermissions: Boolean = false,
    val showPermissionError: Boolean = false,
    val isProcessingVideo: Boolean = false,
    val errorSheetState: SheetState,
)
