package com.yral.shared.features.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
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
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.copy_profile_name
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.edit_profile
import yral_mobile.shared.libs.designsystem.generated.resources.profile_placeholder
import yral_mobile.shared.libs.designsystem.generated.resources.unique_id
import yral_mobile.shared.libs.designsystem.generated.resources.username
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun EditProfileScreen(
    component: EditProfileComponent,
    viewModel: EditProfileViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                    viewModel.onUsernameFocusChanged(false)
                },
            )
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
    onCopy: () -> Unit,
    onDismissEditing: () -> Unit,
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
                .onFocusChanged { focusState -> onFocusChanged(focusState.isFocused) },
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(YralColors.Neutral900)
                        .border(1.dp, YralColors.Neutral700, shape)
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
                            color = YralColors.NeutralTextTertiary,
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
