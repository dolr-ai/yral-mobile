package com.yral.shared.features.chat.ui.conversation.audio

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

private const val MS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60
private const val TWO_DIGIT_THRESHOLD = 10

@Composable
internal fun AudioRecordingBar(
    elapsedMs: Long,
    isFinalizing: Boolean,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
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
                ).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseDot()
            Text(
                text = formatElapsed(elapsedMs),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
            )
            Text(
                text = if (isFinalizing) "Finalizing..." else "Recording",
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextTertiary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CancelButton(onCancel, enabled = !isFinalizing)
            StopButton(onStop, enabled = !isFinalizing)
        }
    }
}

@Composable
private fun PulseDot() {
    val transition = rememberInfiniteTransition(label = "rec-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 600),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "rec-pulse-alpha",
    )
    Box(
        modifier =
            Modifier
                .size(10.dp)
                .alpha(alpha)
                .background(color = YralColors.Pink300, shape = CircleShape),
    )
}

@Composable
private fun StopButton(
    onStop: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .background(color = YralColors.Pink300, shape = CircleShape)
                .clickable(enabled = enabled, onClick = onStop),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(color = YralColors.Neutral0, shape = RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun CancelButton(
    onCancel: () -> Unit,
    enabled: Boolean,
) {
    Text(
        text = "Cancel",
        style = LocalAppTopography.current.regRegular,
        color = YralColors.NeutralTextSecondary,
        modifier = Modifier.clickable(enabled = enabled, onClick = onCancel),
    )
}

internal fun formatElapsed(ms: Long): String {
    val totalSec = (ms / MS_PER_SECOND).coerceAtLeast(0)
    val min = totalSec / SECONDS_PER_MINUTE
    val sec = totalSec % SECONDS_PER_MINUTE
    val secStr = if (sec < TWO_DIGIT_THRESHOLD) "0$sec" else sec.toString()
    return "$min:$secStr"
}
