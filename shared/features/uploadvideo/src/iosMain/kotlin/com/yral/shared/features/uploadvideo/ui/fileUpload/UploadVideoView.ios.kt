package com.yral.shared.features.uploadvideo.ui.fileUpload

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

actual fun formatMaxDuration(duration: Double): String {
    // STUB implementation
    return duration.toString()
}

@Composable
actual fun formatFileSize(
    bytes: Long,
    precision: Int,
): String {
    // STUB implementation
    return bytes.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun SelectVideoView(
    maxSeconds: Int,
    onVideoSelected: (String) -> Unit,
    onCTAClicked: () -> Unit,
) {
    // STUB implementation

    val errorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectionState =
        VideoSelectionState(
            shouldLaunchPicker = false,
            hasRequestedPermissions = false,
            showPermissionError = false,
            isProcessingVideo = false,
            errorSheetState = errorSheetState,
        )

    VideoSelectionContent(
        maxSeconds = maxSeconds,
        hasPermissions = true,
        selectionState = selectionState,
        onLaunchVideoPicker = {
            onCTAClicked()
            // stub
        },
        onRequestPermissions = {
            onCTAClicked()
            // stub
        },
    )
}
