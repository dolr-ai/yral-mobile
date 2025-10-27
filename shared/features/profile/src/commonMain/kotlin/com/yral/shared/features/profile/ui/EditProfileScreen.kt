package com.yral.shared.features.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.EditProfileViewState
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.bio
import yral_mobile.shared.libs.designsystem.generated.resources.copy_profile_name
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.edit_profile
import yral_mobile.shared.libs.designsystem.generated.resources.edit_profile_icon
import yral_mobile.shared.libs.designsystem.generated.resources.email_id
import yral_mobile.shared.libs.designsystem.generated.resources.enter_bio
import yral_mobile.shared.libs.designsystem.generated.resources.profile_placeholder
import yral_mobile.shared.libs.designsystem.generated.resources.save_changes
import yral_mobile.shared.libs.designsystem.generated.resources.take_photo
import yral_mobile.shared.libs.designsystem.generated.resources.unique_id
import yral_mobile.shared.libs.designsystem.generated.resources.upload_camera_profile
import yral_mobile.shared.libs.designsystem.generated.resources.upload_photo
import yral_mobile.shared.libs.designsystem.generated.resources.upload_profile_image
import yral_mobile.shared.libs.designsystem.generated.resources.uploading_profile_picture
import yral_mobile.shared.libs.designsystem.generated.resources.username
import yral_mobile.shared.libs.designsystem.generated.resources.username_error_text
import yral_mobile.shared.libs.designsystem.generated.resources.username_helper_text
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

private const val DRAG_HANDLE_CORNER_PERCENT = 50

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditProfileScreen(
    component: EditProfileComponent,
    viewModel: EditProfileViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    var usernameBounds by remember { mutableStateOf<Rect?>(null) }
    var bioBounds by remember { mutableStateOf<Rect?>(null) }
    val scrollState = rememberScrollState()
    val galleryPicker = rememberProfileImagePicker(viewModel::uploadProfileImage)
    val photoCapture = rememberProfilePhotoCapture(viewModel::uploadProfileImage)
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPickerSheet by remember { mutableStateOf(false) }
    val handleSave: () -> Unit = {
        val isValid = viewModel.validateCurrentUsername()
        if (isValid) {
            focusManager.clearFocus()
            viewModel.saveProfileChanges()
        }
    }
    val handleBack =
        remember(viewModel, component) {
            {
                viewModel.discardUnsavedProfileEdits()
                component.onBack()
            }
        }

    val profileImageToast = state.profileImageToastMessage
    if (profileImageToast != null) {
        val message = stringResource(profileImageToast)
        LaunchedEffect(message) {
            ToastManager.showSuccess(type = ToastType.Small(message = message))
            viewModel.consumeProfileImageToast()
        }
    }

    BackHandler(onBack = handleBack)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.discardUnsavedProfileEdits()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        LaunchedEffect(showPickerSheet) {
            if (!showPickerSheet) {
                bottomSheetState.hide()
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .pointerInput(state.isUsernameFocused, usernameBounds, bioBounds) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            val up = waitForUpOrCancellation()
                            val position = (up ?: down).position
                            val inUsername = usernameBounds?.contains(position) == true
                            val inBio = bioBounds?.contains(position) == true

                            if (!inUsername && !inBio) {
                                focusManager.clearFocus()
                            }
                        }
                    },
        ) {
            EditProfileHeader(onBack = handleBack)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(36.dp))
                ProfileImage(
                    imageUrl = state.profileImageUrl,
                    isUploading = state.isUploadingProfileImage,
                    onEditClick = {
                        if (!state.isUploadingProfileImage) {
                            showPickerSheet = true
                        }
                    },
                )
                Spacer(modifier = Modifier.height(40.dp))
                UsernameSection(
                    state = state,
                    onValueChange = viewModel::onUsernameChanged,
                    onFocusChanged = viewModel::onUsernameFocusChanged,
                    onClear = viewModel::clearUsernameInput,
                    onRevert = viewModel::revertUsernameChange,
                    onBoundsChanged = { usernameBounds = it },
                )
                val isInvalid = !state.isUsernameValid
                val customError = state.usernameErrorMessage
                if (customError != null || state.isUsernameFocused) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val message =
                        customError
                            ?: stringResource(
                                if (isInvalid) {
                                    DesignRes.string.username_error_text
                                } else {
                                    DesignRes.string.username_helper_text
                                },
                            )
                    val messageColor =
                        if (customError != null || isInvalid) {
                            YralColors.Red300
                        } else {
                            YralColors.Neutral500
                        }
                    Text(
                        text = message,
                        style = LocalAppTopography.current.baseRegular,
                        color = messageColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                BioSection(
                    bio = state.bioInput,
                    onBioChange = viewModel::onBioChanged,
                    onFocusChanged = viewModel::onBioFocusChanged,
                    isFocused = state.isBioFocused,
                    onBoundsChanged = { bioBounds = it },
                )
                Spacer(modifier = Modifier.height(24.dp))

                UniqueIdSection(
                    uniqueId = state.uniqueId,
                    onCopy = {
                        if (state.uniqueId.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(state.uniqueId))
                        }
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))

                EmailSection(email = state.emailId)
                Spacer(modifier = Modifier.height(24.dp))

                val sanitizedUsername = remember(state.usernameInput) { state.usernameInput.trim().removePrefix("@") }
                val sanitizedBio = remember(state.bioInput) { state.bioInput.trim() }
                val hasPendingChanges =
                    sanitizedUsername != state.initialUsername || sanitizedBio != state.initialBio
                val isButtonEnabled =
                    hasPendingChanges &&
                        state.isUsernameValid &&
                        !state.isSavingProfile &&
                        !state.isUploadingProfileImage
                val saveButtonState =
                    when {
                        state.isSavingProfile || state.isUploadingProfileImage -> YralButtonState.Loading
                        isButtonEnabled -> YralButtonState.Enabled
                        else -> YralButtonState.Disabled
                    }

                YralGradientButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(DesignRes.string.save_changes),
                    buttonState = saveButtonState,
                    onClick = handleSave,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        when {
            state.isUploadingProfileImage -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(YralColors.Neutral950.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        YralLoader(size = 100.dp)
                        Text(
                            text = stringResource(DesignRes.string.uploading_profile_picture),
                            style = LocalAppTopography.current.baseSemiBold,
                            color = YralColors.NeutralTextPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            state.isSavingProfile -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(YralColors.Neutral950.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    YralLoader(size = 100.dp)
                }
            }
        }

        if (showPickerSheet) {
            LaunchedEffect(Unit) {
                bottomSheetState.show()
            }
            ModalBottomSheet(
                sheetState = bottomSheetState,
                onDismissRequest = { showPickerSheet = false },
                containerColor = YralColors.Neutral900,
                dragHandle = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .width(32.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(DRAG_HANDLE_CORNER_PERCENT))
                                    .background(YralColors.Neutral500),
                        )
                    }
                },
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    EditProfileActionButton(
                        textRes = DesignRes.string.upload_photo,
                        iconRes = DesignRes.drawable.upload_profile_image,
                        onClick = {
                            focusManager.clearFocus()
                            showPickerSheet = false
                            galleryPicker()
                        },
                    )
                    EditProfileActionButton(
                        textRes = DesignRes.string.take_photo,
                        iconRes = DesignRes.drawable.upload_camera_profile,
                        onClick = {
                            focusManager.clearFocus()
                            showPickerSheet = false
                            photoCapture()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditProfileHeader(onBack: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = null,
            modifier =
                Modifier
                    .size(24.dp)
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBack),
        )
        Text(
            text = stringResource(DesignRes.string.edit_profile),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfileImage(
    imageUrl: String?,
    isUploading: Boolean,
    onEditClick: () -> Unit,
) {
    val imageSize = 114.dp
    val editIconSize = 32.dp

    Box(
        modifier = Modifier.size(imageSize),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrEmpty()) {
            Image(
                painter = painterResource(DesignRes.drawable.profile_placeholder),
                contentDescription = null,
                modifier = Modifier.size(imageSize),
            )
        } else {
            YralAsyncImage(
                imageUrl = imageUrl,
                modifier =
                    Modifier
                        .size(imageSize)
                        .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier =
                Modifier
                    .size(editIconSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = editIconSize * 0.05f, y = editIconSize * 0.05f)
                    .clip(CircleShape)
                    .background(YralColors.Grey0)
                    .border(width = 1.dp, color = YralColors.Neutral700, shape = CircleShape)
                    .clickable(enabled = !isUploading, onClick = onEditClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(DesignRes.drawable.edit_profile_icon),
                contentDescription = stringResource(DesignRes.string.edit_profile),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                colorFilter = ColorFilter.tint(YralColors.Neutral900),
            )
        }
    }
}

@Composable
private fun EditProfileActionButton(
    textRes: StringResource,
    iconRes: DrawableResource,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val textStyle =
        LocalAppTopography.current.baseSemiBold.copy(
            fontSize = 14.sp,
            color = YralColors.NeutralTextPrimary,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(shape)
                .background(YralColors.Neutral800)
                .border(1.dp, YralColors.Neutral700, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(textRes),
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UsernameSection(
    state: EditProfileViewState,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    onRevert: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(DesignRes.string.username),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        Spacer(modifier = Modifier.height(8.dp))
        UsernameTextField(
            state = state,
            onValueChange = onValueChange,
            onFocusChanged = onFocusChanged,
            onClear = onClear,
            onRevert = onRevert,
            onBoundsChanged = onBoundsChanged,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun UsernameTextField(
    state: EditProfileViewState,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    onRevert: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.shouldFocusUsername) {
        if (state.shouldFocusUsername) {
            focusRequester.requestFocus()
        }
    }
    BasicTextField(
        value = state.usernameInput,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = LocalAppTopography.current.baseSemiBold.copy(color = YralColors.NeutralTextPrimary),
        cursorBrush = SolidColor(YralColors.NeutralTextPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    if (state.usernameErrorMessage != null || !state.isUsernameValid) {
                        onRevert()
                    }
                    focusManager.clearFocus()
                },
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState -> onFocusChanged(focusState.isFocused) }
                .onGloballyPositioned { coordinates ->
                    onBoundsChanged(coordinates.boundsInRoot())
                },
        decorationBox = { innerTextField ->
            val isInvalid = !state.isUsernameValid
            val borderColor =
                when {
                    isInvalid -> YralColors.Red300
                    state.isUsernameFocused -> YralColors.Pink300
                    else -> YralColors.Neutral700
                }
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(YralColors.Neutral900)
                        .border(1.dp, borderColor, shape)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    innerTextField()
                }
                if (state.isUsernameFocused) {
                    Box(
                        modifier =
                            Modifier
                                .size(28.dp)
                                .clickable(onClick = onClear),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(DesignRes.drawable.cross),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun BioSection(
    bio: String,
    onBioChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    isFocused: Boolean,
    onBoundsChanged: (Rect) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(DesignRes.string.bio),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val shape = RoundedCornerShape(12.dp)
        BasicTextField(
            value = bio,
            onValueChange = onBioChange,
            textStyle = LocalAppTopography.current.baseRegular.copy(color = YralColors.NeutralTextPrimary),
            cursorBrush = SolidColor(YralColors.NeutralTextPrimary),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .onGloballyPositioned { coordinates ->
                        onBoundsChanged(coordinates.boundsInRoot())
                    }.onFocusChanged { focusState -> onFocusChanged(focusState.isFocused) },
            decorationBox = { innerTextField ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(YralColors.Neutral900)
                            .border(
                                width = 1.dp,
                                color = if (isFocused) YralColors.Pink300 else YralColors.Neutral700,
                                shape = shape,
                            ).padding(horizontal = 10.dp, vertical = 12.dp),
                ) {
                    if (bio.isEmpty()) {
                        Text(
                            text = stringResource(DesignRes.string.enter_bio),
                            style = LocalAppTopography.current.baseRegular,
                            color = YralColors.NeutralTextTertiary,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun UniqueIdSection(
    uniqueId: String,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(DesignRes.string.unique_id),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val shape = RoundedCornerShape(12.dp)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(shape)
                    .background(YralColors.Neutral900)
                    .border(1.dp, YralColors.Neutral700, shape)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = uniqueId,
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextTertiary,
                modifier = Modifier.weight(1f),
                maxLines = 3,
            )
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clickable { onCopy() },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(DesignRes.drawable.copy_profile_name),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EmailSection(email: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(DesignRes.string.email_id),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(shape)
                    .background(YralColors.Neutral900)
                    .border(1.dp, YralColors.Neutral700, shape)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = email,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextTertiary,
            )
        }
    }
}
