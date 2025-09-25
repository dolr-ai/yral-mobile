package com.yral.shared.reportVideo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.reportVideo.domain.models.ReportVideoData
import com.yral.shared.reportVideo.domain.models.VideoReportReason
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.reportvideo.generated.resources.Res
import yral_mobile.shared.features.reportvideo.generated.resources.add_details
import yral_mobile.shared.features.reportvideo.generated.resources.please_provide_more_details
import yral_mobile.shared.features.reportvideo.generated.resources.reason_nudity
import yral_mobile.shared.features.reportvideo.generated.resources.reason_offensive
import yral_mobile.shared.features.reportvideo.generated.resources.reason_others
import yral_mobile.shared.features.reportvideo.generated.resources.reason_spam
import yral_mobile.shared.features.reportvideo.generated.resources.reason_violence
import yral_mobile.shared.features.reportvideo.generated.resources.report_submitted
import yral_mobile.shared.features.reportvideo.generated.resources.report_submitted_thank_you
import yral_mobile.shared.features.reportvideo.generated.resources.report_video
import yral_mobile.shared.features.reportvideo.generated.resources.report_video_question
import yral_mobile.shared.features.reportvideo.generated.resources.submit
import yral_mobile.shared.libs.designsystem.generated.resources.exclamation
import yral_mobile.shared.libs.designsystem.generated.resources.radio_selected
import yral_mobile.shared.libs.designsystem.generated.resources.radio_unselected
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun ReportVideo(
    modifier: Modifier = Modifier,
    onReportClicked: () -> Unit,
) {
    Box(modifier = modifier) {
        Image(
            modifier =
                Modifier
                    .size(36.dp)
                    .padding(1.dp)
                    .clickable { onReportClicked() },
            painter = painterResource(DesignRes.drawable.exclamation),
            contentDescription = "report video",
            contentScale = ContentScale.None,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportVideoSheet(
    onDismissRequest: () -> Unit,
    bottomSheetState: SheetState,
    isLoading: Boolean,
    reasons: List<VideoReportReason>,
    onSubmit: (ReportVideoData) -> Unit,
) {
    val successMessage =
        stringResource(Res.string.report_submitted) to
            stringResource(Res.string.report_submitted_thank_you)
    var selectedReason by remember { mutableStateOf<VideoReportReason?>(null) }
    var text by remember { mutableStateOf("") }
    val buttonState =
        when {
            isLoading -> YralButtonState.Loading
            selectedReason == null -> YralButtonState.Disabled
            selectedReason == VideoReportReason.OTHERS && text.isEmpty() -> YralButtonState.Disabled
            else -> YralButtonState.Enabled
        }
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = bottomSheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(
                        start = 16.dp,
                        top = 28.dp,
                        end = 16.dp,
                        bottom = 36.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VideoReportSheetTitle()
            VideoReportReasons(
                modifier = Modifier.weight(1f, fill = false),
                reasons = reasons,
                selectedReason = selectedReason,
                onSelected = { selectedReason = it },
                text = text,
                onTextUpdate = { text = it },
            )
            YralGradientButton(
                text = stringResource(Res.string.submit),
                buttonState = buttonState,
            ) {
                selectedReason?.let {
                    onSubmit(
                        ReportVideoData(
                            reason = it,
                            otherReasonText = text,
                            successMessage = successMessage,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoReportSheetTitle() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.report_video),
            style = LocalAppTopography.current.xlSemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.report_video_question),
            style = LocalAppTopography.current.regRegular,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VideoReportReasons(
    modifier: Modifier,
    reasons: List<VideoReportReason>,
    selectedReason: VideoReportReason?,
    onSelected: (reason: VideoReportReason) -> Unit,
    text: String,
    onTextUpdate: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    var inputHeight by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedReason) {
        if (selectedReason == VideoReportReason.OTHERS) {
            listState.animateScrollToItem(reasons.size, inputHeight)
        }
    }
    LaunchedEffect(text) {
        // automatically scroll to end of column as text grows
        if (selectedReason == VideoReportReason.OTHERS) {
            listState.scrollToItem(reasons.size, inputHeight)
        }
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        items(reasons) { reason ->
            ReasonItem(
                reason = reason,
                isSelected = reason.name == selectedReason?.name,
                onClick = {
                    onSelected(reason)
                },
            )
        }
        if (selectedReason == VideoReportReason.OTHERS) {
            item {
                ReasonDetailsInput(
                    text = text,
                    onValueChange = onTextUpdate,
                    onHeightChange = { inputHeight = it },
                )
            }
        }
    }
}

@Composable
private fun ReasonDetailsInput(
    text: String,
    onValueChange: (text: String) -> Unit,
    onHeightChange: (height: Int) -> Unit,
) {
    Column(
        modifier =
            Modifier.onGloballyPositioned {
                onHeightChange(it.size.height)
            },
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.please_provide_more_details),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        TextField(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
            value = text,
            onValueChange = onValueChange,
            colors =
                TextFieldDefaults.colors().copy(
                    focusedTextColor = YralColors.Neutral300,
                    unfocusedTextColor = YralColors.Neutral300,
                    disabledTextColor = YralColors.Neutral600,
                    focusedContainerColor = YralColors.Neutral800,
                    unfocusedContainerColor = YralColors.Neutral800,
                    disabledContainerColor = YralColors.Neutral800,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            textStyle = LocalAppTopography.current.baseRegular,
            placeholder = {
                Text(
                    text = stringResource(Res.string.add_details),
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.Neutral600,
                )
            },
        )
    }
}

@Composable
private fun ReasonItem(
    reason: VideoReportReason,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(color = YralColors.Neutral800, shape = RoundedCornerShape(size = 4.dp))
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier.size(18.dp),
            painter =
                painterResource(
                    if (isSelected) {
                        DesignRes.drawable.radio_selected
                    } else {
                        DesignRes.drawable.radio_unselected
                    },
                ),
            contentDescription = "image description",
            contentScale = ContentScale.None,
        )
        Text(
            text = reason.displayText(),
            style = LocalAppTopography.current.baseMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun VideoReportReason.displayText(): String =
    when (this) {
        VideoReportReason.NUDITY_PORN -> stringResource(Res.string.reason_nudity)
        VideoReportReason.VIOLENCE -> stringResource(Res.string.reason_violence)
        VideoReportReason.OFFENSIVE -> stringResource(Res.string.reason_offensive)
        VideoReportReason.SPAM -> stringResource(Res.string.reason_spam)
        VideoReportReason.OTHERS -> stringResource(Res.string.reason_others)
    }
