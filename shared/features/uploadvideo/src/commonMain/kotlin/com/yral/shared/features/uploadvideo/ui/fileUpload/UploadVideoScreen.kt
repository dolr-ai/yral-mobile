package com.yral.shared.features.uploadvideo.ui.fileUpload

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.yral.shared.features.uploadvideo.nav.fileUpload.UploadVideoComponent
import com.yral.shared.features.uploadvideo.presentation.UploadVideoViewModel
import com.yral.shared.features.uploadvideo.ui.components.hashtagInput.HashtagInput
import com.yral.shared.features.uploadvideo.ui.components.hashtagInput.keyboardHeightAsState
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.videoPlayer.ResizeMode
import com.yral.shared.libs.videoPlayer.YralVideoPlayer
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.add_hashtag
import yral_mobile.shared.features.uploadvideo.generated.resources.caption
import yral_mobile.shared.features.uploadvideo.generated.resources.enter_caption_here
import yral_mobile.shared.features.uploadvideo.generated.resources.upload
import yral_mobile.shared.features.uploadvideo.generated.resources.uploading_message
import yral_mobile.shared.features.uploadvideo.generated.resources.uploading_progress
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.upload_video
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

private const val TOTAL_ITEMS = 5

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Suppress("MagicNumber", "LongMethod")
@Composable
fun UploadVideoScreen(
    component: UploadVideoComponent,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: UploadVideoViewModel = koinViewModel(),
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.pushScreenView() }
    LaunchedEffect(key1 = Unit) {
        viewModel.eventsFlow.collectLatest { value ->
            component.processEvent(value)
        }
    }

    val listState = rememberLazyListState()
    val keyboardHeight by keyboardHeightAsState()
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > 0) {
            listState.animateScrollToItem(TOTAL_ITEMS - 1)
        }
    }
    BackHandler(enabled = viewState.uploadUiState !is UiState.Initial, onBack = { })
    when (val uploadUiState = viewState.uploadUiState) {
        UiState.Initial -> {
            UploadVideoIdle(
                listState = listState,
                bottomPadding = bottomPadding,
                modifier = modifier,
                viewState = viewState,
                viewModel = viewModel,
                onBack = { component.onBack() },
                promptLogin = { component.promptLogin() },
            )
        }

        is UiState.InProgress -> {
            UploadVideoProgress(modifier, uploadUiState, viewState)
        }

        is UiState.Success<*> -> {
            UploadVideoSuccess(onDone = viewModel::onGoToHomeClicked)
        }

        is UiState.Failure -> {
            LaunchedEffect(Unit) { viewModel.pushUploadFailed(uploadUiState.error) }
            @Suppress("ForbiddenComment")
            UploadVideoFailure(
                onTryAgain = viewModel::onRetryClicked,
                onGotoHome = viewModel::onGoToHomeClicked,
            )
        }
    }
}

@Composable
private fun UploadVideoIdle(
    listState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier,
    viewState: UploadVideoViewModel.ViewState,
    viewModel: UploadVideoViewModel,
    onBack: () -> Unit,
    promptLogin: () -> Unit,
) {
    val density = LocalDensity.current
    val imeBottomDp = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val keyboardAwareBottomPadding = (imeBottomDp - bottomPadding).coerceAtLeast(0.dp)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = keyboardAwareBottomPadding),
    ) {
        // Update TOTAL_ITEMS if adding any more items
        item { Header(onBack) }
        item {
            UploadVideo(
                viewState.selectedFilePath ?: "",
                onVideoSelected = viewModel::onFileSelected,
                onCTAClicked = { viewModel.pushSelectFile() },
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            Submit(
                enabled = viewState.canUpload,
                onClick = {
                    if (viewState.isLoggedIn) {
                        viewModel.onUploadButtonClicked()
                    } else {
                        promptLogin()
                    }
                },
            )
        }
    }
}

@Composable
private fun UploadVideoProgress(
    modifier: Modifier,
    uploadUiState: UiState.InProgress,
    viewState: UploadVideoViewModel.ViewState,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        Header()
        UploadProgressView(uploadUiState.progress)
        YralVideoPlayer(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            url = checkNotNull(viewState.selectedFilePath),
            autoPlay = true,
            videoResizeMode = ResizeMode.FIXED_WIDTH,
            onError = { error ->
                Logger.d("Video error: $error")
            },
        )
    }
}

@Composable
private fun Header(onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onBack?.let {
            Image(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = "back button",
                contentScale = ContentScale.None,
                modifier = Modifier.size(24.dp).clickable { onBack() },
            )
        }
        Text(
            text = stringResource(DesignRes.string.upload_video),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun UploadProgressView(progress: Float) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(Res.string.uploading_message),
            style = LocalAppTopography.current.regRegular,
            color = YralColors.NeutralTextPrimary,
        )
        Spacer(Modifier.height(16.dp))
        UploadProgressBar(progress)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.uploading_progress, (progress * 100).toInt()),
            style = LocalAppTopography.current.regRegular,
            color = YralColors.NeutralTextSecondary,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun UploadProgressBar(progress: Float) {
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
                    .fillMaxWidth(progress)
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
}

@Suppress("UnusedPrivateMember")
@Composable
private fun VideoDetails(
    caption: String,
    hashtags: List<String>,
    onCaptionChanged: (caption: String) -> Unit,
    onHashtagsChanged: (hashtags: List<String>) -> Unit,
) {
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
                text = stringResource(Res.string.caption),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.Neutral300,
            )
            CaptionInput(caption, onCaptionChanged)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(Res.string.add_hashtag),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.Neutral300,
            )
            HashtagInput(hashtags, onHashtagsChanged)
        }
    }
}

@Composable
fun Submit(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        YralGradientButton(
            text = stringResource(Res.string.upload),
            buttonState = if (enabled) YralButtonState.Enabled else YralButtonState.Disabled,
            onClick = onClick,
        )
    }
}

@Composable
private fun CaptionInput(
    text: String,
    onValueChange: (String) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    TextField(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .onFocusChanged {
                    isFocused = it.isFocused
                }.border(
                    width = 1.dp,
                    color = if (isFocused) YralColors.Neutral500 else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                ).clip(RoundedCornerShape(8.dp)),
        value = text,
        onValueChange = onValueChange,
        colors =
            TextFieldDefaults.colors().copy(
                focusedTextColor = YralColors.Neutral300,
                unfocusedTextColor = YralColors.Neutral300,
                disabledTextColor = YralColors.Neutral600,
                focusedContainerColor = YralColors.Neutral900,
                unfocusedContainerColor = YralColors.Neutral900,
                disabledContainerColor = YralColors.Neutral900,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        textStyle = LocalAppTopography.current.baseRegular,
        placeholder = {
            Text(
                text = stringResource(Res.string.enter_caption_here),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.Neutral600,
            )
        },
    )
}
