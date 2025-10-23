package com.yral.shared.features.uploadvideo.ui.fileUpload

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun SelectVideoView(
    maxSeconds: Int,
    onVideoSelected: (String) -> Unit,
    onCTAClicked: () -> Unit,
) {
    // STUB implementation

//    val errorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isProcessingVideo by remember { mutableStateOf(false) }

    VideoSelectionContent(
        maxSeconds = maxSeconds,
        isProcessingVideo = isProcessingVideo,
        onSelectFileClick = {
            if (!isProcessingVideo) {
                onCTAClicked()
                // stub
            }
        },
    )
}
