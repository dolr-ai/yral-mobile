package com.yral.shared.features.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.EditProfileViewState
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.bio
import yral_mobile.shared.libs.designsystem.generated.resources.copy_profile_name
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.edit_profile
import yral_mobile.shared.libs.designsystem.generated.resources.email_id
import yral_mobile.shared.libs.designsystem.generated.resources.enter_bio
import yral_mobile.shared.libs.designsystem.generated.resources.profile_placeholder
import yral_mobile.shared.libs.designsystem.generated.resources.save_changes
import yral_mobile.shared.libs.designsystem.generated.resources.unique_id
import yral_mobile.shared.libs.designsystem.generated.resources.username
import yral_mobile.shared.libs.designsystem.generated.resources.username_error_text
import yral_mobile.shared.libs.designsystem.generated.resources.username_helper_text
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod", "CyclomaticComplexMethod")
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
    val handleSave: () -> Unit = {
        val isValid = viewModel.validateCurrentUsername()
        if (isValid) {
            focusManager.clearFocus()
            viewModel.saveProfileChanges()
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(scrollState)
                .pointerInput(state.isUsernameFocused, usernameBounds, bioBounds) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val up = waitForUpOrCancellation()
                        val position = (up ?: down).position
                        val inUsername = usernameBounds?.contains(position) == true
                        val inBio = bioBounds?.contains(position) == true

                        if (!inUsername && !inBio) {
                            if (state.isUsernameFocused) {
                                viewModel.revertUsernameChange()
                            }
                            focusManager.clearFocus()
                        }
                    }
                },
    ) {
        EditProfileHeader(onBack = component::onBack)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            ProfileImage(state.profileImageUrl)
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

            YralGradientButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(DesignRes.string.save_changes),
                onClick = handleSave,
            )
            Spacer(modifier = Modifier.height(24.dp))
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
private fun ProfileImage(imageUrl: String?) {
    val size = 114.dp
    if (imageUrl.isNullOrEmpty()) {
        Image(
            painter = painterResource(DesignRes.drawable.profile_placeholder),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    } else {
        YralAsyncImage(
            imageUrl = imageUrl,
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape),
            contentScale = ContentScale.Crop,
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
                    },
            decorationBox = { innerTextField ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(YralColors.Neutral900)
                            .border(1.dp, YralColors.Neutral700, shape)
                            .padding(horizontal = 10.dp, vertical = 12.dp),
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
