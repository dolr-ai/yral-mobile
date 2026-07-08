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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralContextMenu
import com.yral.shared.libs.designsystem.component.YralContextMenuItem
import com.yral.shared.libs.designsystem.component.limitTextLength
import com.yral.shared.libs.designsystem.component.rememberTextFieldValueState
import com.yral.shared.libs.designsystem.component.withNativeTextInput
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.camera
import yral_mobile.shared.features.chat.generated.resources.message_placeholder
import yral_mobile.shared.features.chat.generated.resources.photo_library
import yral_mobile.shared.features.chat.generated.resources.request_image
import yral_mobile.shared.features.chat.generated.resources.send
import yral_mobile.shared.features.chat.generated.resources.voice_message
import yral_mobile.shared.libs.designsystem.generated.resources.ic_camera
import yral_mobile.shared.libs.designsystem.generated.resources.ic_gallery
import yral_mobile.shared.libs.designsystem.generated.resources.ic_microphone
import yral_mobile.shared.libs.designsystem.generated.resources.ic_plus_circle
import yral_mobile.shared.libs.designsystem.generated.resources.ic_send
import yral_mobile.shared.libs.designsystem.generated.resources.ic_send_disabled
import yral_mobile.shared.libs.designsystem.generated.resources.ic_thunder
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

private const val MAX_CHARACTER_LIMIT = 4000
private const val MAX_LINES = 5
private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60
private const val TIMER_DIGITS = 2

// The collage-request cooldown counts down to the next 04:00 UTC pre-gen —
// up to a full day — so include hours; the m:ss formatter alone would
// render e.g. "1439:59".
private fun formatRequestImageCooldown(totalSeconds: Int): String {
    val hours = totalSeconds.coerceAtLeast(0) / SECONDS_PER_HOUR
    if (hours <= 0) return formatRemainingMmSs(totalSeconds)
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val minutesStr = minutes.toString().padStart(TIMER_DIGITS, '0')
    val secondsStr = seconds.toString().padStart(TIMER_DIGITS, '0')
    return "$hours:$minutesStr:$secondsStr"
}

@Composable
internal fun ChatInputArea(
    input: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: (() -> Unit)? = null,
    onGalleryClick: (() -> Unit)? = null,
    onRequestImageClick: (() -> Unit)? = null,
    // Non-null disables the Request Image option and shows this cooldown.
    requestImageRemainingSeconds: Int? = null,
    onMicClick: (() -> Unit)? = null,
    showAttachmentMenu: Boolean = true,
    placeholder: String? = null,
    hasWaitingAssistant: Boolean = false,
) {
    val defaultPlaceholder = stringResource(Res.string.message_placeholder)
    val finalPlaceholder = placeholder ?: defaultPlaceholder
    val textFieldValueState = rememberTextFieldValueState(input)
    val inputText = textFieldValueState.value.text
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
            value = textFieldValueState.value,
            onValueChange = { newValue ->
                val limitedValue = newValue.limitTextLength(MAX_CHARACTER_LIMIT)
                textFieldValueState.value = limitedValue
                onInputChange(limitedValue.text)
            },
            textStyle = LocalAppTopography.current.baseRegular.copy(color = YralColors.NeutralTextPrimary),
            keyboardOptions = KeyboardOptions.Default.withNativeTextInput(),
            cursorBrush = SolidColor(YralColors.Pink300),
            maxLines = MAX_LINES,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    if (inputText.isEmpty()) {
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
            inputText,
            onSendClick,
            showAttachmentMenu,
            onCameraClick,
            onGalleryClick,
            onRequestImageClick,
            requestImageRemainingSeconds,
            onMicClick,
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
    onRequestImageClick: (() -> Unit)?,
    requestImageRemainingSeconds: Int?,
    onMicClick: (() -> Unit)?,
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
        } else if (showAttachmentMenu) {
            if (onCameraClick != null && onGalleryClick != null) {
                // Show attachment menu (camera/gallery) when both callbacks provided
                YralContextMenu(
                    items =
                        buildList {
                            add(
                                YralContextMenuItem(
                                    text = stringResource(Res.string.camera),
                                    icon = DesignRes.drawable.ic_camera,
                                    onClick = onCameraClick,
                                ),
                            )
                            add(
                                YralContextMenuItem(
                                    text = stringResource(Res.string.photo_library),
                                    icon = DesignRes.drawable.ic_gallery,
                                    onClick = onGalleryClick,
                                ),
                            )
                            if (onRequestImageClick != null) {
                                add(
                                    YralContextMenuItem(
                                        text = stringResource(Res.string.request_image),
                                        icon = DesignRes.drawable.ic_thunder,
                                        onClick = onRequestImageClick,
                                        enabled = requestImageRemainingSeconds == null,
                                        trailingText =
                                            requestImageRemainingSeconds
                                                ?.let(::formatRequestImageCooldown),
                                    ),
                                )
                            }
                        },
                    triggerIcon = DesignRes.drawable.ic_plus_circle,
                    triggerSize = 24.dp,
                    menuIconSize = 20.dp,
                    menuPadding = PaddingValues(bottom = 16.dp),
                )
            }
            if (onMicClick != null) {
                MicButton(onClick = onMicClick)
            }
        } else {
            // Always show send button when attachment menu is disabled (for image preview)
            SendButton(
                enabled = !hasWaitingAssistant,
                onSendClick = onSendClick,
            )
        }
    }
}

@Composable
private fun MicButton(onClick: () -> Unit) {
    Image(
        painter = painterResource(DesignRes.drawable.ic_microphone),
        contentDescription = stringResource(Res.string.voice_message),
        modifier =
            Modifier
                .size(24.dp)
                .clickable(onClick = onClick),
        contentScale = ContentScale.None,
    )
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
