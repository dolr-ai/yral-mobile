package com.yral.shared.features.uploadvideo.ui.aiVideoGen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.yral.shared.features.subscriptions.nav.SubscriptionNudgeContent
import com.yral.shared.features.subscriptions.ui.components.BoltIcon
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoErrorType
import com.yral.shared.features.uploadvideo.nav.aiVideoGen.AiVideoGenComponent
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenViewModel
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenViewModel.BottomSheetType
import com.yral.shared.features.uploadvideo.presentation.AiVideoGenerationMode
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.LoaderSize
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.videoPlayer.YralVideoPlayer
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.add_image
import yral_mobile.shared.features.uploadvideo.generated.resources.ai_video_authentication_failed
import yral_mobile.shared.features.uploadvideo.generated.resources.ai_video_insufficient_balance
import yral_mobile.shared.features.uploadvideo.generated.resources.ai_video_invalid_input
import yral_mobile.shared.features.uploadvideo.generated.resources.ai_video_provider_error
import yral_mobile.shared.features.uploadvideo.generated.resources.ai_video_rate_limit_exceeded
import yral_mobile.shared.features.uploadvideo.generated.resources.ai_video_service_unavailable
import yral_mobile.shared.features.uploadvideo.generated.resources.create_ai_video
import yral_mobile.shared.features.uploadvideo.generated.resources.generate_video
import yral_mobile.shared.features.uploadvideo.generated.resources.image_to_video
import yral_mobile.shared.features.uploadvideo.generated.resources.play_games
import yral_mobile.shared.features.uploadvideo.generated.resources.selected_model_does_not_support_image_input
import yral_mobile.shared.features.uploadvideo.generated.resources.text_to_video
import yral_mobile.shared.features.uploadvideo.generated.resources.to_earn_token
import yral_mobile.shared.features.uploadvideo.generated.resources.toast_ai_video_generating
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_an_image_to_guide_your_ai_video_style
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_completed_message
import yral_mobile.shared.features.uploadvideo.generated.resources.upload_successful
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.coins
import yral_mobile.shared.libs.designsystem.generated.resources.done
import yral_mobile.shared.libs.designsystem.generated.resources.ic_error
import yral_mobile.shared.libs.designsystem.generated.resources.ic_gallery
import yral_mobile.shared.libs.designsystem.generated.resources.ic_x
import yral_mobile.shared.libs.designsystem.generated.resources.my_profile
import yral_mobile.shared.libs.designsystem.generated.resources.pink_gradient_background
import yral_mobile.shared.libs.designsystem.generated.resources.something_went_wrong
import yral_mobile.shared.libs.designsystem.generated.resources.try_again
import yral_mobile.shared.libs.designsystem.generated.resources.your_videos
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    LaunchedEffect(Unit) {
        viewModel.aiVideoGenEvents.collect { event ->
            when (event) {
                is AiVideoGenViewModel.AiVideoGenEvent.ShowSubscriptionNudge -> {
                    component.subscriptionCoordinator.showSubscriptionNudge(
                        content =
                            SubscriptionNudgeContent(
                                title = event.title,
                                description = event.description,
                                topContent = { BoltIcon() },
                                entryPoint = event.entryPoint,
                            ),
                    )
                }

                is AiVideoGenViewModel.AiVideoGenEvent.RefreshProDetails -> {
                    component.subscriptionCoordinator.refreshCreditBalances()
                }

                is AiVideoGenViewModel.AiVideoGenEvent.ShowGeneratedToast -> {
                    ToastManager.showSuccess(
                        type =
                            ToastType.Small(
                                getString(Res.string.toast_ai_video_generating),
                            ),
                    )
                }

                is AiVideoGenViewModel.AiVideoGenEvent.NavigateToHome -> {
                    viewModel.cleanup()
                    component.goToHome()
                }
            }
        }
    }
    BackHandler(
        enabled = viewState.uiState is UiState.Success || viewState.uiState is UiState.InProgress,
        onBack = {
            viewModel.cleanup()
            if (viewState.uiState is UiState.InProgress) {
                component.onBack()
            } else {
                component.goToHome()
            }
        },
    )
    Column(modifier.fillMaxSize()) {
        when (viewState.uiState) {
            UiState.Initial, is UiState.InProgress -> {
                Header {
                    viewModel.cleanup()
                    component.onBack()
                }
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState()),
                ) {
                    PromptScreen(
                        viewState = viewState,
                        viewModel = viewModel,
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

            is UiState.Failure -> {
                Unit
            } // No op since failure is shown in a bottomSheet
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
        is BottomSheetType.Error -> {
            val dismissSheet = {
                viewModel.setBottomSheetType(BottomSheetType.None)
            }
            val tryAgain = {
                viewModel.setBottomSheetType(BottomSheetType.None)
                if (viewState.isLoggedIn) {
                    viewModel.tryAgain()
                } else {
                    component.promptLogin()
                }
            }
            GenerationErrorPrompt(
                title = sheetType.title,
                message = sheetType.message,
                bottomSheetState = bottomSheetState,
                dismissSheet = dismissSheet,
                tryAgain = tryAgain,
            )
        }

        is BottomSheetType.None -> {
            Unit
        }
    }
}

@Composable
private fun PromptScreen(
    viewState: AiVideoGenViewModel.ViewState,
    viewModel: AiVideoGenViewModel,
    goToHome: () -> Unit,
    promptLogin: () -> Unit,
) {
    val openImagePicker =
        rememberAiVideoImagePicker { bytes ->
            viewModel.updateSelectedImage(bytes)
        }
    val buttonState =
        remember(viewState.usedCredits, viewState.prompt, viewState.uiState) {
            if (viewState.uiState is UiState.InProgress) {
                YralButtonState.Loading
            } else if (viewModel.shouldEnableButton()) {
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
            GenerationModeTabs(
                selectedMode = viewState.generationMode,
                onModeSelected = viewModel::updateGenerationMode,
            )
            if (viewState.generationMode == AiVideoGenerationMode.IMAGE_TO_VIDEO) {
                ImageInputPanel(
                    imageBytes = viewState.selectedImageBytes,
                    isImageInputSupported = viewState.selectedProvider?.supportsImage == true,
                    onPickImage = openImagePicker,
                    onClearImage = viewModel::clearSelectedImage,
                )
            }
            PromptInput(
                text = viewState.prompt,
                onValueChange = { viewModel.updatePromptText(it) },
                onHeightChange = {},
            )
            CreditsDetails(viewState = viewState)
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
        if (!viewState.isCreditsAvailable() && viewState.isBalanceLow()) {
            Spacer(Modifier.height(16.dp))
            PlayGameText(goToHome)
        }
    }
}

@Composable
private fun GenerationModeTabs(
    selectedMode: AiVideoGenerationMode,
    onModeSelected: (AiVideoGenerationMode) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Neutral800, RoundedCornerShape(24.dp))
                .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GenerationModeTab(
            mode = AiVideoGenerationMode.IMAGE_TO_VIDEO,
            selectedMode = selectedMode,
            text = stringResource(Res.string.image_to_video),
            onModeSelected = onModeSelected,
            modifier = Modifier.weight(1f),
        )
        GenerationModeTab(
            mode = AiVideoGenerationMode.TEXT_TO_VIDEO,
            selectedMode = selectedMode,
            text = stringResource(Res.string.text_to_video),
            onModeSelected = onModeSelected,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GenerationModeTab(
    mode: AiVideoGenerationMode,
    selectedMode: AiVideoGenerationMode,
    text: String,
    onModeSelected: (AiVideoGenerationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedMode == mode
    Box(
        modifier =
            modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isSelected) Color.White else Color.Transparent)
                .clickable { onModeSelected(mode) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalAppTopography.current.baseBold,
            color = if (isSelected) YralColors.Neutral900 else YralColors.Neutral300,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ImageInputPanel(
    imageBytes: ByteArray?,
    isImageInputSupported: Boolean,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(YralColors.Neutral900)
                    .border(
                        width = 1.dp,
                        color = YralColors.Neutral700,
                        shape = RoundedCornerShape(8.dp),
                    ).clickable(enabled = isImageInputSupported) { onPickImage() },
            contentAlignment = Alignment.Center,
        ) {
            if (imageBytes == null) {
                AddImagePlaceholder(isImageInputSupported)
            } else {
                YralAsyncImage(
                    imageUrl = imageBytes,
                    modifier = Modifier.fillMaxSize(),
                    loaderSize = LoaderSize.Fixed,
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(8.dp),
                )
                Image(
                    painter = painterResource(DesignRes.drawable.ic_x),
                    contentDescription = "remove image",
                    contentScale = ContentScale.None,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(24.dp)
                            .clickable { onClearImage() },
                )
            }
        }
        if (!isImageInputSupported) {
            Text(
                text = stringResource(Res.string.selected_model_does_not_support_image_input),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.Red300,
            )
        }
    }
}

@Composable
private fun AddImagePlaceholder(isImageInputSupported: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.ic_gallery),
            contentDescription = "add image",
            contentScale = ContentScale.None,
            modifier = Modifier.size(36.dp),
            alpha = if (isImageInputSupported) 1f else 0.45f,
        )
        Text(
            text = stringResource(Res.string.add_image),
            style = LocalAppTopography.current.mdBold,
            color = if (isImageInputSupported) YralColors.NeutralTextPrimary else YralColors.Neutral600,
        )
        Text(
            text = stringResource(Res.string.upload_an_image_to_guide_your_ai_video_style),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Neutral500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerationErrorPrompt(
    title: GenerateVideoErrorType?,
    message: String,
    bottomSheetState: SheetState,
    dismissSheet: () -> Unit,
    tryAgain: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = dismissSheet,
        bottomSheetState = bottomSheetState,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(46.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .padding(start = 16.dp, top = 36.dp, end = 16.dp, bottom = 36.dp),
        ) {
            Text(
                text = title.toTitle(),
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
private fun GenerateVideoErrorType?.toTitle(): String =
    when (this) {
        GenerateVideoErrorType.INVALID_INPUT -> stringResource(Res.string.ai_video_invalid_input)
        GenerateVideoErrorType.AUTHENTICATION_FAILED -> stringResource(Res.string.ai_video_authentication_failed)
        GenerateVideoErrorType.INSUFFICIENT_BALANCE -> stringResource(Res.string.ai_video_insufficient_balance)
        GenerateVideoErrorType.RATE_LIMIT_EXCEEDED -> stringResource(Res.string.ai_video_rate_limit_exceeded)
        GenerateVideoErrorType.PROVIDER_ERROR -> stringResource(Res.string.ai_video_provider_error)
        GenerateVideoErrorType.SERVICE_UNAVAILABLE -> stringResource(Res.string.ai_video_service_unavailable)
        null -> stringResource(DesignRes.string.something_went_wrong)
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

object AiVideoGenScreenConstants {
    const val ARROW_ROTATION = -90f
    const val PROMPT_MAX_CHAR = 500
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
