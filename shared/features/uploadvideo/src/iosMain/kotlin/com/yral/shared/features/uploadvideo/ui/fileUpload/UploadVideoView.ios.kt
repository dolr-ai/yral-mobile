package com.yral.shared.features.uploadvideo.ui.fileUpload

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.yral.shared.features.uploadvideo.utils.VideoValidator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
internal actual fun SelectVideoView(
    maxSeconds: Int,
    onVideoSelected: (String) -> Unit,
    onCTAClicked: () -> Unit,
) {
    var pickerError by remember { mutableStateOf<VideoPickerError?>(null) }
    var isProcessingVideo by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val onVideoSelectedState by rememberUpdatedState(onVideoSelected)

    var coordinator by remember { mutableStateOf<IosVideoPickerCoordinator?>(null) }
    val videoValidator = koinInject<VideoValidator>()

    VideoSelectionContent(
        maxSeconds = maxSeconds,
        isProcessingVideo = isProcessingVideo,
        onSelectFileClick = {
            if (!isProcessingVideo) {
                onCTAClicked()
                pickerError = null
                val newCoordinator =
                    IosVideoPickerCoordinator(
                        maxSeconds = maxSeconds,
                        coroutineScope = coroutineScope,
                        videoValidator = videoValidator,
                        onProcessingStateChange = { isProcessingVideo = it },
                        onVideoSelected = { path -> onVideoSelectedState(path) },
                        onError = { error -> pickerError = error },
                        onDismiss = { coordinator = null },
                    )
                val isPresented = newCoordinator.presentPicker()
                if (isPresented) {
                    coordinator = newCoordinator
                } else {
                    pickerError = VideoPickerError.ProcessingFailed
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
