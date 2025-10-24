package com.yral.shared.features.uploadvideo.ui.fileUpload

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.yral.shared.features.uploadvideo.utils.VideoValidationError
import com.yral.shared.features.uploadvideo.utils.VideoValidator
import com.yral.shared.features.uploadvideo.utils.formatFileSize
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.videoPlayer.YralVideoPlayer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.permission_required_description
import yral_mobile.shared.features.uploadvideo.generated.resources.select_file
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_video_to_share_with_world
import yral_mobile.shared.features.uploadvideo.generated.resources.video_file_max_seconds
import yral_mobile.shared.features.uploadvideo.generated.resources.video_validation_duration_exceeds_limit_with_data
import yral_mobile.shared.features.uploadvideo.generated.resources.video_validation_file_size_exceeds_limit_with_data
import yral_mobile.shared.features.uploadvideo.generated.resources.video_validation_processing_failed
import yral_mobile.shared.features.uploadvideo.generated.resources.video_validation_unable_to_read_duration
import yral_mobile.shared.features.uploadvideo.generated.resources.video_validation_unable_to_read_file_size
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.error
import yral_mobile.shared.libs.designsystem.generated.resources.go_to_settings
import yral_mobile.shared.libs.designsystem.generated.resources.ok
import yral_mobile.shared.libs.designsystem.generated.resources.permission_required_title
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

const val UPLOAD_BOX_ASPECT_RATIO = 0.75f

@Composable
fun UploadVideo(
    videoFilePath: String,
    onVideoSelected: (String) -> Unit,
    onCTAClicked: () -> Unit,
) {
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
                        painter = painterResource(DesignRes.drawable.cross),
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

@Composable
internal expect fun SelectVideoView(
    maxSeconds: Int = VideoValidator.VIDEO_MAX_DURATION_SECONDS.toInt(),
    onVideoSelected: (String) -> Unit,
    onCTAClicked: () -> Unit,
)

sealed class VideoPickerError {
    data class Validation(
        val error: VideoValidationError,
    ) : VideoPickerError()
    object ProcessingFailed : VideoPickerError()
}

@Composable
internal fun VideoSelectionContent(
    maxSeconds: Int,
    isProcessingVideo: Boolean,
    onSelectFileClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.upload_video_to_share_with_world),
            style = LocalAppTopography.current.mdMedium,
            color = YralColors.NeutralTextPrimary,
        )
        Text(
            text = stringResource(Res.string.video_file_max_seconds, maxSeconds),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.NeutralTextSecondary,
        )
        Spacer(Modifier.height(0.dp))
        VideoSelectionButton(
            isProcessingVideo = isProcessingVideo,
            onSelectFileClick = onSelectFileClick,
        )
    }
}

@Composable
private fun VideoSelectionButton(
    isProcessingVideo: Boolean,
    onSelectFileClick: () -> Unit,
) {
    YralButton(
        modifier =
            Modifier
                .widthIn(min = 107.dp),
        text =
            if (isProcessingVideo) {
                ""
            } else {
                stringResource(Res.string.select_file)
            },
        borderWidth = 1.dp,
        borderColor = YralColors.Pink300,
        backgroundColor = YralColors.Neutral900,
        textStyle = TextStyle(color = YralColors.Pink300),
        buttonState = if (isProcessingVideo) YralButtonState.Loading else YralButtonState.Enabled,
        onClick = onSelectFileClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoSelectionPermissionErrorDialog(
    sheetState: SheetState,
    onDismissError: () -> Unit,
    onGoToSettingsClicked: () -> Unit,
) {
    YralErrorMessage(
        title = stringResource(DesignRes.string.permission_required_title),
        error = stringResource(Res.string.permission_required_description),
        sheetState = sheetState,
        cta = stringResource(DesignRes.string.go_to_settings),
        onClick = {
            onDismissError()
            onGoToSettingsClicked()
        },
        onDismiss = onDismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoSelectionPickerErrorDialog(
    sheetState: SheetState,
    pickerError: VideoPickerError,
    onDismissError: () -> Unit,
) {
    YralErrorMessage(
        title = stringResource(DesignRes.string.error),
        error = pickerError.toErrorMessage(),
        sheetState = sheetState,
        cta = stringResource(DesignRes.string.ok),
        onClick = onDismissError,
        onDismiss = onDismissError,
    )
}

@Composable
private fun VideoPickerError.toErrorMessage(): String =
    when (this) {
        is VideoPickerError.Validation ->
            when (val error = this.error) {
                is VideoValidationError.UnableToReadDuration ->
                    stringResource(Res.string.video_validation_unable_to_read_duration)
                is VideoValidationError.UnableToReadFileSize ->
                    stringResource(Res.string.video_validation_unable_to_read_file_size)
                is VideoValidationError.DurationExceedsLimit ->
                    stringResource(
                        Res.string.video_validation_duration_exceeds_limit_with_data,
                        error.limit.toInt(),
                    )
                is VideoValidationError.FileSizeExceedsLimit ->
                    stringResource(
                        Res.string.video_validation_file_size_exceeds_limit_with_data,
                        formatFileSize(error.limit, precision = 0),
                    )
            }

        is VideoPickerError.ProcessingFailed -> stringResource(Res.string.video_validation_processing_failed)
    }
