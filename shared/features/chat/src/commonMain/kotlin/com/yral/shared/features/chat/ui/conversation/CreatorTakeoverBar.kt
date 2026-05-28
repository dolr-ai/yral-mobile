package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.takeover_input_placeholder
import yral_mobile.shared.features.chat.generated.resources.takeover_timer_label
import yral_mobile.shared.features.chat.generated.resources.takeover_toggle_active
import yral_mobile.shared.features.chat.generated.resources.takeover_toggle_inactive

private const val URGENT_THRESHOLD_SECONDS = 30
private const val SECONDS_PER_MINUTE = 60

@Composable
internal fun CreatorTakeoverBar(
    isActive: Boolean,
    isStarting: Boolean,
    isEnding: Boolean,
    isMessageSending: Boolean,
    remainingSeconds: Int,
    creatorDisplayName: String,
    onToggleOn: () -> Unit,
    onToggleOff: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    val toggleEnabled = !isStarting && !isEnding
    val toggleLabel =
        if (isActive) {
            stringResource(Res.string.takeover_toggle_active)
        } else {
            stringResource(Res.string.takeover_toggle_inactive, creatorDisplayName)
        }
    val timerText = formatRemainingMmSs(remainingSeconds)
    val isUrgent = remainingSeconds in 1..URGENT_THRESHOLD_SECONDS

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isActive) YralColors.Neutral900 else Color.Transparent,
                        shape = RoundedCornerShape(28.dp),
                    ).border(
                        width = 1.dp,
                        color = if (isActive) YralColors.PrimaryYellow else YralColors.Neutral700,
                        shape = RoundedCornerShape(28.dp),
                    ).clickable(enabled = toggleEnabled) {
                        if (isActive) onToggleOff() else onToggleOn()
                    }.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = toggleLabel,
                color = if (isActive) YralColors.PrimaryYellow else Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            if (isActive) {
                Text(
                    text = stringResource(Res.string.takeover_timer_label, timerText),
                    color = if (isUrgent) YralColors.Red300 else Color.White.copy(alpha = 0.8f),
                    fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (isUrgent) 18.sp else 14.sp,
                )
            }
        }

        if (isActive) {
            Spacer(modifier = Modifier.height(8.dp))
            ChatInputArea(
                input = draft,
                onInputChange = { draft = it },
                onSendClick = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty() && !isMessageSending) {
                        onSend(trimmed)
                        draft = ""
                    }
                },
                showAttachmentMenu = false,
                placeholder = stringResource(Res.string.takeover_input_placeholder, creatorDisplayName),
                hasWaitingAssistant = isMessageSending,
            )
        }
    }
}

private fun formatRemainingMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / SECONDS_PER_MINUTE
    val seconds = safe % SECONDS_PER_MINUTE
    val secondsStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minutes:$secondsStr"
}
