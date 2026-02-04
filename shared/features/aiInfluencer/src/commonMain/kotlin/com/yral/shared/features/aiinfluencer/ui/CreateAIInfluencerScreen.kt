package com.yral.shared.features.aiinfluencer.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.aiinfluencer.viewmodel.AiInfluencerStep
import com.yral.shared.features.aiinfluencer.viewmodel.AiInfluencerUiState
import com.yral.shared.features.aiinfluencer.viewmodel.AiInfluencerViewModel
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.auth.ui.LoginMode
import com.yral.shared.features.auth.ui.LoginScreenType
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.auth.ui.rememberLoginInfo
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.aiinfluencer.generated.resources.Res
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_create_profile
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_description_label
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_error_generic
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_loading_metadata_subtitle
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_loading_metadata_title
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_loading_prompt_subtitle
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_loading_prompt_title
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_name_label
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_next
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_profile_title
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_prompt_label
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_prompt_placeholder
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_prompt_title
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_review_subtitle
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_review_title
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_take_photo
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_title
import yral_mobile.shared.features.aiinfluencer.generated.resources.ai_influencer_upload_photo
import yral_mobile.shared.features.aiinfluencer.generated.resources.create_influencer_magic
import yral_mobile.shared.features.aiinfluencer.generated.resources.create_influencer_puzzle
import yral_mobile.shared.features.aiinfluencer.generated.resources.create_influencer_take_photo
import yral_mobile.shared.features.aiinfluencer.generated.resources.create_influencer_upload_photo
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.edit_profile_icon
import yral_mobile.shared.libs.designsystem.generated.resources.ic_camera
import yral_mobile.shared.libs.designsystem.generated.resources.ic_gallery
import yral_mobile.shared.libs.designsystem.generated.resources.profile_placeholder
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@Composable
fun CreateAIInfluencerScreen(
    modifier: Modifier = Modifier,
    viewModel: AiInfluencerViewModel = koinViewModel(),
    sessionManager: SessionManager = koinInject(),
    requestLoginFactory: RequestLoginFactory? = null,
    onBack: () -> Unit = {},
    onCreateProfile: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val galleryPicker = rememberAiInfluencerImagePicker(viewModel::onAvatarSelected)
    val photoCapture = rememberAiInfluencerPhotoCapture(viewModel::onAvatarSelected)
    val isSocialSignedIn by
        sessionManager
            .observeSessionPropertyWithDefault(
                selector = { it.isSocialSignIn },
                defaultValue = false,
            ).collectAsStateWithLifecycle(initialValue = false)
    val loginState =
        requestLoginFactory?.let {
            rememberLoginInfo(requestLoginFactory = it, key = "ai-influencer-login")
        }
    val backgroundModifier = modifier.fillMaxSize().background(Color.Black)
    val handleBack: () -> Unit = {
        val handled = viewModel.onBack()
        if (!handled) {
            onBack()
        }
    }
    val isLoggedIn = isSocialSignedIn && sessionManager.userPrincipal != null
    val requestLogin =
        remember(loginState) {
            {
                loginState?.requestLogin(
                    SignupPageName.CREATE_INFLUENCER,
                    LoginScreenType.BottomSheet(LoginBottomSheetType.CREATE_INFLUENCER),
                    LoginMode.BOTH,
                    null,
                    null,
                ) {}
            }
        }

    Box(
        modifier = backgroundModifier,
    ) {
        when (val step = state.step) {
            is AiInfluencerStep.DescriptionEntry ->
                PromptEntryScreen(
                    state = state,
                    onBack = onBack,
                    onDescriptionChange = viewModel::onPromptChanged,
                    onNext = {
                        if (isLoggedIn) {
                            viewModel.submitPrompt()
                        } else {
                            requestLogin()
                        }
                    },
                )

            is AiInfluencerStep.LoadingPrompt ->
                LoadingScreen(
                    title = stringResource(Res.string.ai_influencer_loading_prompt_title),
                    subtitle = stringResource(Res.string.ai_influencer_loading_prompt_subtitle),
                    icon = Res.drawable.create_influencer_magic,
                )

            is AiInfluencerStep.PersonaReview ->
                PersonaReviewScreen(
                    state = state,
                    onBack = handleBack,
                    onTextChange = viewModel::onPersonaChanged,
                    onNext = viewModel::submitPersona,
                )

            is AiInfluencerStep.LoadingMetadata ->
                LoadingScreen(
                    title = stringResource(Res.string.ai_influencer_loading_metadata_title),
                    subtitle = stringResource(Res.string.ai_influencer_loading_metadata_subtitle),
                    icon = Res.drawable.create_influencer_puzzle,
                )

            is AiInfluencerStep.ProfileDetails ->
                ProfileDetailsScreen(
                    state = state,
                    onBack = handleBack,
                    onNameChange = viewModel::onProfileNameChanged,
                    onDescriptionChange = viewModel::onProfileDescriptionChanged,
                    onEditImage = viewModel::openImagePicker,
                    onCreateProfile = onCreateProfile,
                )
        }

        if (state.isImagePickerVisible) {
            ImagePickerSheet(
                onDismiss = viewModel::dismissImagePicker,
                onUploadPhoto = {
                    galleryPicker()
                    viewModel.dismissImagePicker()
                },
                onTakePhoto = {
                    photoCapture()
                    viewModel.dismissImagePicker()
                },
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun PromptEntryScreen(
    state: AiInfluencerUiState,
    onBack: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val step = state.step as AiInfluencerStep.DescriptionEntry
    val buttonState =
        if (step.description.isNotBlank()) {
            YralButtonState.Enabled
        } else {
            YralButtonState.Disabled
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(onBack = onBack, title = stringResource(Res.string.ai_influencer_title))
        Spacer(modifier = Modifier.height(22.dp))
        StepIndicator(
            activeSteps = 1,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(30.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(Res.string.ai_influencer_prompt_title),
                style = LocalAppTopography.current.xxlBold,
                color = YralColors.Grey50,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.ai_influencer_prompt_label),
                    style = LocalAppTopography.current.baseMedium,
                    color = YralColors.Neutral200,
                )
                Text(
                    text = "${step.description.length}/$DESCRIPTION_CHAR_LIMIT",
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.Neutral500,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = step.description,
                onValueChange = onDescriptionChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                placeholder = {
                    Text(
                        text = stringResource(Res.string.ai_influencer_prompt_placeholder),
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.Neutral600,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = YralColors.Neutral900,
                        unfocusedContainerColor = YralColors.Neutral900,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = YralColors.Pink300,
                        focusedTextColor = YralColors.Grey50,
                        unfocusedTextColor = YralColors.Grey50,
                    ),
                textStyle = LocalAppTopography.current.baseRegular,
            )
            ErrorText(state.errorMessage)
        }
        Spacer(modifier = Modifier.weight(1f))
        YralGradientButton(
            text = stringResource(Res.string.ai_influencer_next),
            buttonState = buttonState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            onClick = onNext,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun PersonaReviewScreen(
    state: AiInfluencerUiState,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val step = state.step as AiInfluencerStep.PersonaReview
    val buttonState =
        if (step.editedInstructions.isNotBlank()) {
            YralButtonState.Enabled
        } else {
            YralButtonState.Disabled
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(onBack = onBack, title = stringResource(Res.string.ai_influencer_title))
        Spacer(modifier = Modifier.height(22.dp))
        StepIndicator(activeSteps = 2, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(30.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(Res.string.ai_influencer_review_title),
                style = LocalAppTopography.current.xxlBold,
                color = YralColors.Grey50,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.ai_influencer_review_subtitle),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.Neutral500,
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                value = step.editedInstructions,
                onValueChange = onTextChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                placeholder = {
                    Text(
                        text = stringResource(Res.string.ai_influencer_prompt_placeholder),
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.Neutral600,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = YralColors.Neutral900,
                        unfocusedContainerColor = YralColors.Neutral900,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = YralColors.Pink300,
                        focusedTextColor = YralColors.Grey50,
                        unfocusedTextColor = YralColors.Grey50,
                    ),
                textStyle = LocalAppTopography.current.baseRegular,
            )
            ErrorText(state.errorMessage)
        }
        Spacer(modifier = Modifier.weight(1f))
        YralGradientButton(
            text = stringResource(Res.string.ai_influencer_next),
            buttonState = buttonState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            onClick = onNext,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun ProfileDetailsScreen(
    state: AiInfluencerUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onEditImage: () -> Unit,
    onCreateProfile: () -> Unit,
) {
    val step = state.step as AiInfluencerStep.ProfileDetails
    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(24.dp),
            ) {
                Icon(
                    painter = painterResource(DesignRes.drawable.arrow_left),
                    contentDescription = null,
                    tint = YralColors.Grey50,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = stringResource(Res.string.ai_influencer_title),
                style = LocalAppTopography.current.xlBold,
                color = YralColors.Grey50,
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        StepIndicator(activeSteps = 3, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(30.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AvatarBlock(step = step, onEditImage = onEditImage, size = 150.dp)
        }
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = stringResource(Res.string.ai_influencer_profile_title),
            style = LocalAppTopography.current.xxlBold,
            color = YralColors.Grey0,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ProfileTextField(
                value = step.displayName.ifBlank { step.name },
                label = stringResource(Res.string.ai_influencer_name_label),
                onValueChange = onNameChange,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProfileTextField(
                value = step.description,
                label = stringResource(Res.string.ai_influencer_description_label),
                onValueChange = onDescriptionChange,
                minLines = 2,
            )
            ErrorText(state.errorMessage)
        }
        Spacer(modifier = Modifier.weight(1f))
        YralGradientButton(
            text = stringResource(Res.string.ai_influencer_create_profile),
            buttonState =
                if (state.isBotCreationLoading) {
                    YralButtonState.Loading
                } else {
                    YralButtonState.Enabled
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            onClick = onCreateProfile,
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun AvatarBlock(
    step: AiInfluencerStep.ProfileDetails,
    onEditImage: () -> Unit,
    size: Dp = 180.dp,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(YralColors.Neutral800),
                contentAlignment = Alignment.Center,
            ) {
                if (step.avatarBytes != null) {
                    YralAsyncImage(
                        imageUrl = step.avatarBytes,
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                    )
                } else if (step.avatarUrl.isNotBlank()) {
                    YralAsyncImage(
                        imageUrl = step.avatarUrl,
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                    )
                } else {
                    Image(
                        painter = painterResource(DesignRes.drawable.profile_placeholder),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            IconButton(
                onClick = onEditImage,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(width = 2.dp, color = Color.Black, shape = CircleShape),
            ) {
                Icon(
                    painter = painterResource(DesignRes.drawable.edit_profile_icon),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(
    title: String,
    subtitle: String,
    icon: DrawableResource,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .padding(horizontal = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(YralColors.NeutralBlack),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(160.dp),
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = title,
            style = LocalAppTopography.current.xlSemiBold,
            color = YralColors.Grey50,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            style = LocalAppTopography.current.mdRegular,
            color = YralColors.Neutral500,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScreenHeader(
    onBack: () -> Unit,
    title: String,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onBack,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(24.dp),
        ) {
            Icon(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = null,
                tint = YralColors.Grey50,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            style = LocalAppTopography.current.xlBold,
            color = YralColors.Grey50,
        )
    }
}

@Composable
private fun StepIndicator(
    activeSteps: Int,
    totalSteps: Int = 3,
    inactiveColor: Color = YralColors.Neutral800,
    modifier: Modifier = Modifier,
) {
    val activeColors = listOf(YralColors.Pink200, YralColors.Pink300, YralColors.Pink400)
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < activeSteps
            val segmentColor = if (isActive) activeColors.getOrElse(index) { YralColors.Pink400 } else inactiveColor
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(segmentColor),
            )
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = YralColors.Neutral900,
                    unfocusedContainerColor = YralColors.Neutral900,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = YralColors.Pink300,
                    focusedTextColor = YralColors.Grey50,
                    unfocusedTextColor = YralColors.Grey50,
                ),
            textStyle = LocalAppTopography.current.baseMedium,
            minLines = minLines,
        )
    }
}

@Composable
private fun ErrorText(errorMessage: String?) {
    if (errorMessage.isNullOrBlank()) return
    Text(
        text = errorMessage.ifBlank { stringResource(Res.string.ai_influencer_error_generic) },
        style = LocalAppTopography.current.smRegular,
        color = YralColors.ErrorRed,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerSheet(
    onDismiss: () -> Unit,
    onUploadPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = YralColors.Neutral900,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 54.dp, height = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(YralColors.Neutral700),
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BottomSheetButton(
                icon = Res.drawable.create_influencer_upload_photo,
                text = stringResource(Res.string.ai_influencer_upload_photo),
                onClick = onUploadPhoto,
            )
            BottomSheetButton(
                icon = Res.drawable.create_influencer_take_photo,
                text = stringResource(Res.string.ai_influencer_take_photo),
                onClick = onTakePhoto,
            )
        }
    }
}

@Composable
private fun BottomSheetButton(
    icon: DrawableResource,
    text: String,
    onClick: () -> Unit,
    height: Dp = 42.dp,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .border(width = 1.dp, color = YralColors.Neutral700, shape = RoundedCornerShape(8.dp))
                .background(YralColors.Neutral800)
                .clickable { onClick() }
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = text,
                style = LocalAppTopography.current.mdSemiBold,
                color = YralColors.Grey50,
            )
        }
    }
}

private const val DESCRIPTION_CHAR_LIMIT = 200
