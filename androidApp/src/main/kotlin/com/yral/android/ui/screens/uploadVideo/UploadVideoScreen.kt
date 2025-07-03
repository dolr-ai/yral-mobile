package com.yral.android.ui.screens.uploadVideo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.widgets.YralButtonState
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.video.YralVideoPlayer

private const val TOTAL_ITEMS = 5

@Suppress("MagicNumber")
@Composable
fun UploadVideoScreen(modifier: Modifier = Modifier) {
    var videoFilePath by remember { mutableStateOf("") }
    val isUploadInProgress by remember { mutableStateOf(false) }
    val progress by remember { mutableIntStateOf(20) }
    var isUploadResultVisible by remember { mutableStateOf(false) }
    val isUploadResultMessage by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val keyboardHeight by keyboardHeightAsState()
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > 0) {
            listState.animateScrollToItem(TOTAL_ITEMS - 1)
        }
    }
    if (!isUploadResultVisible) {
        LazyColumn(
            state = listState,
            modifier = modifier.imePadding(),
        ) {
            // Update TOTAL_ITEMS if adding any more items
            item { Header() }
            if (isUploadInProgress) {
                item {
                    UploadProgressView(
                        progress = progress,
                        videoFilePath = videoFilePath,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                    )
                }
            } else {
                item {
                    UploadVideo(videoFilePath) {
                        videoFilePath = it
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
                item { VideoDetails() }
                item { Submit() }
            }
        }
    } else {
        if (isUploadResultMessage.isEmpty()) {
            UploadVideoSuccess {
                isUploadResultVisible = false
            }
        } else {
            UploadVideoFailure(
                reason = isUploadResultMessage,
                onTryAgain = { isUploadResultVisible = false },
                onGotoHome = { isUploadResultVisible = false },
            )
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.upload_video),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun UploadProgressView(
    progress: Int,
    videoFilePath: String,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.uploading_message),
            style = LocalAppTopography.current.regRegular,
            color = YralColors.NeutralTextPrimary,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        color = YralColors.Neutral800,
                        shape = RoundedCornerShape(100.dp),
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progress.toFractionOf())
                        .height(10.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            brush =
                                linearGradient(
                                    colors =
                                        listOf(
                                            YralColors.Pink200,
                                            YralColors.Pink300,
                                        ),
                                ),
                            shape = RoundedCornerShape(100.dp),
                        ),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.uploading_progress, progress),
            style = LocalAppTopography.current.regRegular,
            color = YralColors.NeutralTextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        YralVideoPlayer(
            modifier = Modifier.fillMaxSize(),
            url = videoFilePath,
            autoPlay = true,
            onError = { error ->
                Logger.d("Video error: $error")
            },
        )
    }
}

@Composable
private fun VideoDetails() {
    var text by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf(emptyList<String>()) }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.caption),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.Neutral300,
            )
            CaptionInput(text) { text = it }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.add_hashtag),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.Neutral300,
            )
            HashtagInput(hashtags) { hashtags = it }
        }
    }
}

@Composable
fun Submit() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        YralGradientButton(
            text = stringResource(R.string.upload),
            buttonState = YralButtonState.Disabled,
        ) {
        }
    }
}

@Composable
private fun CaptionInput(
    text: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp)
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
                text = stringResource(R.string.enter_caption_here),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.Neutral600,
            )
        },
    )
}

fun Int.toFractionOf(total: Int = 100): Float =
    if (total == 0) {
        0.0f
    } else {
        (this.toFloat() / total)
    }
