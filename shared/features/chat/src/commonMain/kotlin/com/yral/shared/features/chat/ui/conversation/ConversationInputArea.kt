package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralContextMenu
import com.yral.shared.libs.designsystem.component.YralContextMenuItem
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.camera
import yral_mobile.shared.features.chat.generated.resources.message_placeholder
import yral_mobile.shared.features.chat.generated.resources.photo_library
import yral_mobile.shared.features.chat.generated.resources.send
import yral_mobile.shared.libs.designsystem.generated.resources.ic_camera
import yral_mobile.shared.libs.designsystem.generated.resources.ic_gallery
import yral_mobile.shared.libs.designsystem.generated.resources.ic_plus_circle
import yral_mobile.shared.libs.designsystem.generated.resources.ic_send
import yral_mobile.shared.libs.designsystem.generated.resources.ic_send_disabled
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

private const val MAX_CHARACTER_LIMIT = 4000
private const val MAX_LINES = 5

@Composable
internal fun ChatInputArea(
    input: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: (() -> Unit)? = null,
    onGalleryClick: (() -> Unit)? = null,
    showAttachmentMenu: Boolean = true,
    placeholder: String? = null,
    hasWaitingAssistant: Boolean = false,
) {
    val defaultPlaceholder = stringResource(Res.string.message_placeholder)
    val finalPlaceholder = placeholder ?: defaultPlaceholder
    // Input field
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = YralColors.Neutral900,
                    shape = RoundedCornerShape(30.dp),
                ).border(
                    width = 1.dp,
                    color = YralColors.Neutral800,
                    shape = RoundedCornerShape(30.dp),
                ).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            modifier = Modifier.weight(1f),
            value = input,
            onValueChange = { newValue: String ->
                if (newValue.length <= MAX_CHARACTER_LIMIT) {
                    onInputChange(newValue)
                }
            },
            textStyle = LocalAppTopography.current.baseRegular.copy(color = YralColors.NeutralTextPrimary),
            cursorBrush = SolidColor(YralColors.Pink300),
            maxLines = MAX_LINES,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    if (input.isEmpty()) {
                        Text(
                            text = finalPlaceholder,
                            color = YralColors.NeutralTextTertiary,
                            style = LocalAppTopography.current.baseRegular,
                        )
                    }
                    innerTextField()
                }
            },
        )

        InputActions(
            input,
            onSendClick,
            showAttachmentMenu,
            onCameraClick,
            onGalleryClick,
            hasWaitingAssistant,
        )
    }
}

@Composable
private fun InputActions(
    input: String,
    onSendClick: () -> Unit,
    showAttachmentMenu: Boolean,
    onCameraClick: (() -> Unit)?,
    onGalleryClick: (() -> Unit)?,
    hasWaitingAssistant: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (input.isNotBlank()) {
            // Send button when text is entered
            SendButton(
                enabled = input.isNotBlank() && !hasWaitingAssistant,
                onSendClick = onSendClick,
            )
        } else if (showAttachmentMenu && onCameraClick != null && onGalleryClick != null) {
            // Show attachment menu only if enabled and callbacks are provided
            YralContextMenu(
                items =
                    listOf(
                        YralContextMenuItem(
                            text = stringResource(Res.string.camera),
                            icon = DesignRes.drawable.ic_camera,
                            onClick = onCameraClick,
                        ),
                        YralContextMenuItem(
                            text = stringResource(Res.string.photo_library),
                            icon = DesignRes.drawable.ic_gallery,
                            onClick = onGalleryClick,
                        ),
                    ),
                triggerIcon = DesignRes.drawable.ic_plus_circle,
                triggerSize = 24.dp,
                menuIconSize = 20.dp,
                menuPadding = PaddingValues(bottom = 16.dp),
            )
        } else if (!showAttachmentMenu) {
            // Always show send button when attachment menu is disabled (for image preview)
            SendButton(
                enabled = !hasWaitingAssistant,
                onSendClick = onSendClick,
            )
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    onSendClick: () -> Unit,
) {
    Image(
        painter =
            painterResource(
                if (enabled) {
                    DesignRes.drawable.ic_send
                } else {
                    DesignRes.drawable.ic_send_disabled
                },
            ),
        contentDescription = stringResource(Res.string.send),
        modifier =
            Modifier
                .size(24.dp)
                .clickable(enabled = enabled, onClick = onSendClick),
        contentScale = ContentScale.None,
    )
}
