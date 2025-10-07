package com.yral.shared.features.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.EditProfileViewState
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.cancel
import yral_mobile.shared.libs.designsystem.generated.resources.change_name
import yral_mobile.shared.libs.designsystem.generated.resources.change_name_prompt
import yral_mobile.shared.libs.designsystem.generated.resources.copy_profile_name
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.done
import yral_mobile.shared.libs.designsystem.generated.resources.edit_profile
import yral_mobile.shared.libs.designsystem.generated.resources.profile_placeholder
import yral_mobile.shared.libs.designsystem.generated.resources.unique_id
import yral_mobile.shared.libs.designsystem.generated.resources.username
import yral_mobile.shared.libs.designsystem.generated.resources.username_error_text
import yral_mobile.shared.libs.designsystem.generated.resources.username_helper_text
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
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
    val scrollState = rememberScrollState()
    var showConfirmation by remember { mutableStateOf(false) }
    var pendingUsername by remember { mutableStateOf("") }
    val confirmationSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .pointerInput(state.isUsernameFocused, usernameBounds) {
                    if (!state.isUsernameFocused) return@pointerInput
                    detectTapGestures { offset ->
                        val bounds = usernameBounds
                        if (bounds == null || !bounds.contains(offset)) {
                            focusManager.clearFocus()
                        }
                    }
                },
    ) {
        EditProfileHeader(
            onBack = component::onBack,
            showDone = state.isUsernameFocused,
            onDone = {
                val isValid = viewModel.validateCurrentUsername()
                if (isValid) {
                    pendingUsername = viewModel.sanitizedUsername()
                    focusManager.clearFocus()
                    showConfirmation = true
                }
            },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            ProfileImage(state.profileImageUrl)
            Spacer(modifier = Modifier.height(24.dp))
            UsernameSection(
                state = state,
                onValueChange = viewModel::onUsernameChanged,
                onFocusChanged = viewModel::onUsernameFocusChanged,
                onCopy = {
                    val username = state.usernameInput
                    if (username.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString("@" + username))
                    }
                },
                onDismissEditing = {
                    focusManager.clearFocus()
                    viewModel.revertUsernameChange()
                },
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
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(28.dp))
                UniqueIdSection(
                    uniqueId = state.uniqueId,
                    onCopy = {
                        if (state.uniqueId.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(state.uniqueId))
                        }
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showConfirmation) {
        ChangeUsernameConfirmationSheet(
            username = pendingUsername,
            sheetState = confirmationSheetState,
            onConfirm = {
                coroutineScope.launch {
                    confirmationSheetState.hide()
                    viewModel.applyUsernameChange()
                    showConfirmation = false
                    pendingUsername = ""
                }
            },
            onCancel = {
                coroutineScope.launch {
                    confirmationSheetState.hide()
                    viewModel.revertUsernameChange()
                    showConfirmation = false
                    pendingUsername = ""
                }
            },
        )
    }
}

@Composable
private fun EditProfileHeader(
    onBack: () -> Unit,
    onDone: () -> Unit = {},
    showDone: Boolean = false,
) {
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
        if (showDone) {
            Text(
                text = stringResource(DesignRes.string.done),
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.Pink300,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(onClick = onDone),
            )
        }
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
    onCopy: () -> Unit,
    onDismissEditing: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(DesignRes.string.username),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.NeutralTextTertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        UsernameTextField(
            state = state,
            onValueChange = onValueChange,
            onFocusChanged = onFocusChanged,
            onCopy = onCopy,
            onDismissEditing = onDismissEditing,
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
    onCopy: () -> Unit,
    onDismissEditing: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = state.usernameInput,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = LocalAppTopography.current.baseSemiBold.copy(color = YralColors.NeutralTextPrimary),
        cursorBrush = SolidColor(YralColors.NeutralTextPrimary),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onFocusChanged { focusState -> onFocusChanged(focusState.isFocused) }
                .onGloballyPositioned { coordinates ->
                    onBoundsChanged(coordinates.boundsInParent())
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!state.isUsernameFocused) {
                        Text(
                            text = "@",
                            style = LocalAppTopography.current.baseMedium,
                            color =
                                when {
                                    isInvalid -> YralColors.Red300
                                    else -> YralColors.NeutralTextTertiary
                                },
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        innerTextField()
                    }
                }
                val iconPainter =
                    if (state.isUsernameFocused) {
                        painterResource(DesignRes.drawable.cross)
                    } else {
                        painterResource(DesignRes.drawable.copy_profile_name)
                    }
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .clickable {
                                if (state.isUsernameFocused) {
                                    onDismissEditing()
                                } else {
                                    onCopy()
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeUsernameConfirmationSheet(
    username: String,
    sheetState: SheetState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    YralBottomSheet(
        onDismissRequest = onCancel,
        bottomSheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = stringResource(DesignRes.string.change_name_prompt, username),
                style = LocalAppTopography.current.xlSemiBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(40.dp))
            YralGradientButton(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                text = stringResource(DesignRes.string.change_name),
                onClick = onConfirm,
            )
            Spacer(modifier = Modifier.height(12.dp))
            YralButton(
                text = stringResource(DesignRes.string.cancel),
                modifier =
                    Modifier
                        .fillMaxWidth(),
                backgroundColor = YralColors.Neutral800,
                borderColor = YralColors.Neutral700,
                borderWidth = 1.dp,
                textStyle =
                    LocalAppTopography.current.mdMedium.copy(
                        color = YralColors.NeutralTextPrimary,
                        textAlign = TextAlign.Center,
                    ),
                paddingValues = PaddingValues(vertical = 0.dp, horizontal = 0.dp),
                onClick = onCancel,
            )
        }
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
            color = YralColors.NeutralTextTertiary,
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
                color = YralColors.NeutralTextPrimary,
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
