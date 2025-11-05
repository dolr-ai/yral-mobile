package com.yral.shared.features.uploadvideo.ui.aiVideoGen

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.nav.aiVideoGen.AiVideoGenComponent
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenViewModel
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenViewModel.BottomSheetType
import com.yral.shared.features.uploadvideo.ui.aiVideoGen.AiVideoGenScreenConstants.LOADING_MESSAGE_DELAY
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralConfirmationMessage
import com.yral.shared.libs.designsystem.component.YralDragHandle
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.component.getSVGImageModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.videoPlayer.YralVideoPlayer
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.create_ai_video
import yral_mobile.shared.features.uploadvideo.generated.resources.empty_box
import yral_mobile.shared.features.uploadvideo.generated.resources.generate_video
import yral_mobile.shared.features.uploadvideo.generated.resources.generating_video
import yral_mobile.shared.features.uploadvideo.generated.resources.out_of_token
import yral_mobile.shared.features.uploadvideo.generated.resources.out_of_token_desc
import yral_mobile.shared.features.uploadvideo.generated.resources.play_games
import yral_mobile.shared.features.uploadvideo.generated.resources.stay_here
import yral_mobile.shared.features.uploadvideo.generated.resources.this_may_take_few_minutes
import yral_mobile.shared.features.uploadvideo.generated.resources.to_earn_token
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_completed_message
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_successful
import yral_mobile.shared.features.uploadvideo.generated.resources.used_free_ai_video
import yral_mobile.shared.features.uploadvideo.generated.resources.used_free_ai_video_desc
import yral_mobile.shared.features.uploadvideo.generated.resources.video_camera
import yral_mobile.shared.features.uploadvideo.generated.resources.yes_take_me_back
import yral_mobile.shared.features.uploadvideo.generated.resources.you_will_loose_ai_credits
import yral_mobile.shared.features.uploadvideo.generated.resources.you_will_loose_credits_desc
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.coins
import yral_mobile.shared.libs.designsystem.generated.resources.done
import yral_mobile.shared.libs.designsystem.generated.resources.ic_error
import yral_mobile.shared.libs.designsystem.generated.resources.my_profile
import yral_mobile.shared.libs.designsystem.generated.resources.pink_gradient_background
import yral_mobile.shared.libs.designsystem.generated.resources.something_went_wrong
import yral_mobile.shared.libs.designsystem.generated.resources.subscribe_for
import yral_mobile.shared.libs.designsystem.generated.resources.subscribe_now
import yral_mobile.shared.libs.designsystem.generated.resources.try_again
import yral_mobile.shared.libs.designsystem.generated.resources.your_videos
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AiVideoGenScreen(
    component: AiVideoGenComponent,
    modifier: Modifier = Modifier,
    bottomPadding: Dp,
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
                val density = LocalDensity.current
                val imeBottomDp = with(density) { WindowInsets.ime.getBottom(this).toDp() }
                val keyboardAwareBottomPadding = (imeBottomDp - bottomPadding).coerceAtLeast(0.dp)
                val focusManager = LocalFocusManager.current
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = keyboardAwareBottomPadding)
                            .clickable { focusManager.clearFocus(true) },
                ) {
                    PromptScreen(
                        viewState = viewState,
                        viewModel = viewModel,
                        showProvidersSheet = { viewModel.setBottomSheetType(BottomSheetType.ModelSelection) },
                        goToHome = { component.goToHome() },
                        promptLogin = { component.promptLogin() },
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
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                tryAgain = {
                    handleSheetAction {
                        if (viewState.isLoggedIn) {
                            viewModel.tryAgain()
                        } else {
                            component.promptLogin()
                        }
                    }
                },
            )
        }
        is BottomSheetType.BackConfirmation -> {
            YralConfirmationMessage(
                title = stringResource(Res.string.you_will_loose_ai_credits),
                subTitle = stringResource(Res.string.you_will_loose_credits_desc),
                sheetState = bottomSheetState,
                cancel = stringResource(Res.string.yes_take_me_back),
                done = stringResource(Res.string.stay_here),
                onDone = { viewModel.setBottomSheetType(BottomSheetType.None) },
                onCancel = {
                    viewModel.cleanup()
                    component.onBack()
                },
            )
        }
        is BottomSheetType.FreeCreditsUsed -> {
            SubscribePrompt(
                bottomSheetState = extraSheetState,
                dismissSheet = { viewModel.setBottomSheetType(BottomSheetType.None) },
                icon = Res.drawable.video_camera,
                title = stringResource(Res.string.used_free_ai_video),
                subtitle =
                    buildAnnotatedString {
                        val fullMessage = stringResource(Res.string.used_free_ai_video_desc)
                        val firstPart = fullMessage.substringBefore("+").trim()
                        val endPart = fullMessage.substringAfter("+").trim()
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(firstPart)
                        }
                        append(" + ")
                        append(endPart)
                    },
                buttonText = stringResource(DesignRes.string.subscribe_for),
                onSubscribe = { viewModel.setBottomSheetType(BottomSheetType.None) },
            )
        }
        is BottomSheetType.OutOfCredits -> {
            SubscribePrompt(
                bottomSheetState = extraSheetState,
                dismissSheet = { viewModel.setBottomSheetType(BottomSheetType.None) },
                icon = Res.drawable.empty_box,
                title = stringResource(Res.string.out_of_token),
                subtitle =
                    buildAnnotatedString {
                        val fullMessage = stringResource(Res.string.out_of_token_desc)
                        val firstPart = fullMessage.substringBefore("+").trim()
                        val endPart = fullMessage.substringAfter("+").trim()
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(firstPart)
                        }
                        append(" + ")
                        append(endPart)
                    },
                buttonText = stringResource(DesignRes.string.subscribe_now),
                onSubscribe = { viewModel.setBottomSheetType(BottomSheetType.None) },
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
    goToHome: () -> Unit,
    promptLogin: () -> Unit,
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
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 20.dp),
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
                text = stringResource(Res.string.generate_video),
                buttonState = buttonState,
                onClick = {
                    viewModel.createAiVideoClicked()
                    if (viewState.isLoggedIn) {
                        viewModel.generateAiVideo()
                    } else {
                        promptLogin()
                    }
                },
            )
        }
        if (viewState.usedCredits != null && !viewState.isCreditsAvailable() && viewState.isBalanceLow()) {
            Spacer(Modifier.height(16.dp))
            PlayGameText(goToHome)
        }
    }
}

@Composable
private fun PlayGameText(goToHome: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        YralMaskedVectorTextV2(
            text = stringResource(Res.string.play_games),
            drawableRes = DesignRes.drawable.pink_gradient_background,
            textStyle =
                LocalAppTopography
                    .current
                    .mdBold
                    .plus(TextStyle(textAlign = TextAlign.Center)),
            modifier = Modifier.clickable { goToHome() },
        )
        Text(
            text =
                " ".plus(
                    stringResource(
                        Res.string.to_earn_token,
                        stringResource(DesignRes.string.coins),
                    ),
                ),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun GenerationInProgressScreen(
    promptText: String,
    provider: Provider,
) {
    val loadingMessages =
        listOf(
            stringResource(Res.string.generating_video),
            stringResource(Res.string.this_may_take_few_minutes),
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
                text = stringResource(DesignRes.string.something_went_wrong),
                style = LocalAppTopography.current.lgBold,
                color = Color.White,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(DesignRes.drawable.ic_error),
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
                    text = stringResource(DesignRes.string.try_again),
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
                text = stringResource(Res.string.upload_successful),
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
            text = stringResource(DesignRes.string.done),
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
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "back button",
            contentScale = ContentScale.None,
            modifier = Modifier.size(24.dp).clickable { onBack() },
        )
        Text(
            text = stringResource(Res.string.create_ai_video),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscribePrompt(
    bottomSheetState: SheetState,
    dismissSheet: () -> Unit,
    icon: DrawableResource,
    title: String,
    subtitle: AnnotatedString,
    buttonText: String,
    onSubscribe: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = dismissSheet,
        bottomSheetState = bottomSheetState,
        dragHandle = { YralDragHandle() },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 36.dp),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = "icon",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(100.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = LocalAppTopography.current.lgBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = subtitle,
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            YralGradientButton(
                text = buttonText,
                onClick = onSubscribe,
            )
        }
    }
}

object AiVideoGenScreenConstants {
    const val ARROW_ROTATION = -90f
    const val PROMPT_MAX_CHAR = 500
    const val LOADING_MESSAGE_DELAY = 10000L // 10 sec
}

@Composable
private fun buildUploadCompletedMessage(): AnnotatedString {
    val fullMessage = stringResource(Res.string.upload_completed_message)
    val yourVideos = stringResource(DesignRes.string.your_videos)
    val myProfile = stringResource(DesignRes.string.my_profile)
    val firstPart = fullMessage.substringBefore(yourVideos)
    val middlePart = fullMessage.substringAfter(yourVideos).substringBefore(myProfile)
    val endPart = fullMessage.substringAfter(myProfile)
    return buildAnnotatedString {
        append(firstPart)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(DesignRes.string.your_videos))
        }
        append(middlePart)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(DesignRes.string.my_profile))
        }
        append(endPart)
    }
}
