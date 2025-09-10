package com.yral.android.ui.screens.uploadVideo.aiVideoGen

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.account.ErrorMessageSheet
import com.yral.android.ui.screens.account.LoginBottomSheet
import com.yral.android.ui.screens.account.WebViewBottomSheet
import com.yral.android.ui.screens.uploadVideo.aiVideoGen.AiVideoGenScreenConstants.LOADING_MESSAGE_DELAY
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.YralBottomSheet
import com.yral.android.ui.widgets.YralButtonState
import com.yral.android.ui.widgets.YralConfirmationMessage
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.YralLoader
import com.yral.android.ui.widgets.YralMaskedVectorTextV2
import com.yral.android.ui.widgets.getSVGImageModel
import com.yral.android.ui.widgets.video.YralVideoPlayer
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.account.viewmodel.AccountsViewModel.Companion.TERMS_OF_SERVICE_URL
import com.yral.shared.features.account.viewmodel.ErrorType
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenViewModel
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenViewModel.BottomSheetType
import com.yral.shared.libs.arch.presentation.UiState
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVideoGenScreen(
    component: AiVideoGenComponent,
    modifier: Modifier = Modifier,
    viewModel: AiVideoGenViewModel = koinViewModel(),
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val shouldRefresh = viewModel.sessionObserver.collectAsState(null)
    LaunchedEffect(shouldRefresh.value) {
        Logger.d("VideoGen") { "shouldRefresh: $shouldRefresh" }
        shouldRefresh.value?.first?.let { viewModel.refresh(it) }
        shouldRefresh.value?.second?.let { viewModel.updateBalance(it) }
    }
    BackHandler(
        enabled = viewState.uiState is UiState.Success,
        onBack = {
            viewModel.cleanup()
            component.goToHome()
        },
    )
    Column(modifier.fillMaxSize()) {
        when (viewState.uiState) {
            is UiState.InProgress -> {
                Header { viewModel.setBottomSheetType(BottomSheetType.BackConfirmation) }
                viewState.selectedProvider?.let { provider ->
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        GenerationInProgressScreen(
                            promptText = viewState.prompt,
                            provider = provider,
                        )
                    }
                }
            }
            UiState.Initial -> {
                Header {
                    viewModel.cleanup()
                    component.onBack()
                }
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    PromptScreen(
                        viewState = viewState,
                        viewModel = viewModel,
                        showProvidersSheet = { viewModel.setBottomSheetType(BottomSheetType.ModelSelection) },
                    )
                }
            }
            is UiState.Success<*> -> {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    GenerationSuccessScreen(
                        videoUrl = (viewState.uiState as UiState.Success<String>).data,
                        goToHome = {
                            viewModel.cleanup()
                            component.goToHome()
                        },
                    )
                }
            }
            is UiState.Failure -> Unit // No op since failure is shown in a bottomSheet
        }
    }
    AiVideoGenScreenPrompts(component, viewState, viewModel)
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiVideoGenScreenPrompts(
    component: AiVideoGenComponent,
    viewState: AiVideoGenViewModel.ViewState,
    viewModel: AiVideoGenViewModel,
) {
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extraSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    when (val sheetType = viewState.bottomSheetType) {
        is BottomSheetType.ModelSelection -> {
            ModelSelection(
                bottomSheetState = bottomSheetState,
                providers = viewState.providers,
                selectedProvider = viewState.selectedProvider,
                dismissSheet = { viewModel.setBottomSheetType(BottomSheetType.None) },
                setSelectedProvider = { viewModel.selectProvider(it) },
            )
        }
        is BottomSheetType.Error -> {
            val handleSheetAction: (action: () -> Unit) -> Unit = { action ->
                viewModel.setBottomSheetType(BottomSheetType.None)
                if (sheetType.endFlow) {
                    viewModel.cleanup()
                    component.onBack()
                } else {
                    action()
                }
            }
            GenerationErrorPrompt(
                message = sheetType.message,
                bottomSheetState = bottomSheetState,
                dismissSheet = { handleSheetAction { viewModel.resetUi() } },
                tryAgain = { handleSheetAction { viewModel.tryAgain() } },
            )
        }
        is BottomSheetType.Signup -> {
            // reusing from account to be refactored in independent components
            LoginBottomSheet(
                pageName = SignupPageName.VIDEO_CREATION,
                bottomSheetState = bottomSheetState,
                onDismissRequest = { viewModel.setBottomSheetType(BottomSheetType.None) },
                onSignupClicked = { viewModel.signInWithGoogle(context) },
                termsLink = TERMS_OF_SERVICE_URL,
                openTerms = { viewModel.setBottomSheetType(BottomSheetType.Link(TERMS_OF_SERVICE_URL)) },
            )
        }
        is BottomSheetType.Link -> {
            // reusing from account to be refactored in independent components
            LaunchedEffect(sheetType.url) {
                if (sheetType.url.isEmpty()) {
                    extraSheetState.hide()
                } else {
                    extraSheetState.show()
                }
            }
            WebViewBottomSheet(
                link = sheetType.url,
                bottomSheetState = extraSheetState,
                onDismissRequest = { viewModel.setBottomSheetType(BottomSheetType.Signup) },
            )
        }
        is BottomSheetType.SignupFailed -> {
            // reusing from account to be refactored in independent components
            ErrorMessageSheet(
                errorType = ErrorType.SIGNUP_FAILED,
                onDismissRequest = { viewModel.setBottomSheetType(BottomSheetType.None) },
                bottomSheetState = bottomSheetState,
            )
        }
        is BottomSheetType.BackConfirmation -> {
            YralConfirmationMessage(
                title = stringResource(R.string.you_will_loose_ai_credits),
                subTitle = stringResource(R.string.you_will_loose_credits_desc),
                sheetState = bottomSheetState,
                cancel = stringResource(R.string.yes_take_me_back),
                done = stringResource(R.string.stay_here),
                onDone = { viewModel.setBottomSheetType(BottomSheetType.None) },
                onCancel = {
                    viewModel.cleanup()
                    component.onBack()
                },
            )
        }
        is BottomSheetType.None -> Unit
    }
}

@Composable
private fun PromptScreen(
    viewState: AiVideoGenViewModel.ViewState,
    viewModel: AiVideoGenViewModel,
    showProvidersSheet: () -> Unit,
) {
    val buttonState =
        remember(viewState.usedCredits, viewState.prompt) {
            if (viewModel.shouldEnableButton()) {
                YralButtonState.Enabled
            } else {
                YralButtonState.Disabled
            }
        }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        ) {
            PromptInput(
                text = viewState.prompt,
                onValueChange = { viewModel.updatePromptText(it) },
                onHeightChange = {},
            )
            viewState.selectedProvider?.let { provider ->
                ModelDetails(
                    provider = provider,
                    viewState = viewState,
                    onClick = showProvidersSheet,
                )
            }
            YralGradientButton(
                text = stringResource(R.string.generate_video),
                buttonState = buttonState,
                onClick = {
                    viewModel.createAiVideoClicked()
                    viewModel.generateAiVideo()
                },
            )
        }
        if (viewState.usedCredits != null && !viewState.isCreditsAvailable() && viewState.isBalanceLow()) {
            Spacer(Modifier.height(16.dp))
            PlayGameText()
        }
    }
}

@Composable
private fun PlayGameText() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        YralMaskedVectorTextV2(
            text = stringResource(R.string.play_games),
            vectorRes = R.drawable.pink_gradient_background,
            textStyle =
                LocalAppTopography
                    .current
                    .mdBold
                    .plus(TextStyle(textAlign = TextAlign.Center)),
        )
        Text(
            text =
                "".plus(
                    stringResource(
                        R.string.to_earn_token,
                        stringResource(R.string.coins),
                    ),
                ),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Suppress("LongMethod")
@Composable
private fun GenerationInProgressScreen(
    promptText: String,
    provider: Provider,
) {
    val loadingMessages =
        listOf(
            stringResource(R.string.generating_video),
            stringResource(R.string.this_may_take_few_minutes),
        )
    var messageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(LOADING_MESSAGE_DELAY)
            messageIndex = (messageIndex + 1) % loadingMessages.size
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(YralColors.Neutral800, RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = YralColors.Neutral700,
                            shape = RoundedCornerShape(size = 8.dp),
                        ).clip(RoundedCornerShape(8.dp)),
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            ) {
                Text(
                    text = promptText,
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralTextPrimary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                verticalAlignment = Alignment.Bottom,
            ) {
                provider.modelIcon?.let { url ->
                    YralAsyncImage(
                        imageUrl = getSVGImageModel(url),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = provider.name,
                    style = LocalAppTopography.current.regRegular,
                    color = YralColors.NeutralTextPrimary,
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).padding(bottom = 83.dp)) {
            val widthToHeightRatio = 1f / provider.toDefaultAspectRatio()
            val boxHeight = (maxWidth * widthToHeightRatio).coerceAtMost(maxHeight)
            Box(
                modifier =
                    Modifier
                        .height(boxHeight)
                        .fillMaxWidth()
                        .background(YralColors.Neutral800, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(11.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    YralLoader(size = 34.dp)
                    Text(
                        text = loadingMessages[messageIndex],
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.NeutralTextPrimary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerationErrorPrompt(
    message: String,
    bottomSheetState: SheetState,
    dismissSheet: () -> Unit,
    tryAgain: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = dismissSheet,
        bottomSheetState = bottomSheetState,
        dragHandle = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(46.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .padding(start = 16.dp, top = 36.dp, end = 16.dp, bottom = 36.dp),
        ) {
            Text(
                text = stringResource(R.string.something_went_wrong),
                style = LocalAppTopography.current.lgBold,
                color = Color.White,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.upload_error),
                    contentDescription = "error symbol",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(120.dp),
                )
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.Neutral300,
                    )
                }
                YralGradientButton(
                    text = stringResource(R.string.try_again),
                    onClick = tryAgain,
                )
            }
        }
    }
}

@Composable
private fun GenerationSuccessScreen(
    videoUrl: String,
    goToHome: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 38.dp, end = 38.dp, top = 58.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            YralVideoPlayer(
                url = videoUrl,
                modifier =
                    Modifier
                        .width(230.dp)
                        .height(268.dp),
                autoPlay = true,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.upload_successful),
                style = LocalAppTopography.current.lgBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = buildUploadCompletedMessage(),
                style = LocalAppTopography.current.mdRegular,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
        }
        YralGradientButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.done),
            onClick = goToHome,
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.arrow_left),
            contentDescription = "back button",
            contentScale = ContentScale.None,
            modifier = Modifier.size(24.dp).clickable { onBack() },
        )
        Text(
            text = stringResource(R.string.create_ai_video),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

object AiVideoGenScreenConstants {
    const val ARROW_ROTATION = -90f
    const val PROMPT_MAX_CHAR = 500
    const val LOADING_MESSAGE_DELAY = 10000L // 10 sec
}

@Composable
private fun buildUploadCompletedMessage(): AnnotatedString {
    val fullMessage = stringResource(R.string.upload_completed_message)
    val yourVideos = stringResource(R.string.your_videos)
    val myProfile = stringResource(R.string.my_profile)
    val firstPart = fullMessage.substringBefore(yourVideos)
    val middlePart = fullMessage.substringAfter(yourVideos).substringBefore(myProfile)
    val endPart = fullMessage.substringAfter(myProfile)
    return buildAnnotatedString {
        append(firstPart)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.your_videos))
        }
        append(middlePart)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.my_profile))
        }
        append(endPart)
    }
}
