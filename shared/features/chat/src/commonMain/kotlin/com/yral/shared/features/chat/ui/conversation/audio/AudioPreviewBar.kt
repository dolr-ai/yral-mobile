package com.yral.shared.features.chat.ui.conversation.audio

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.ic_audio_pause
import yral_mobile.shared.features.chat.generated.resources.ic_audio_play

private const val MS_PER_SECOND = 1000

@Composable
internal fun AudioPreviewBar(
    attachment: FilePathChatAttachment,
    durationSeconds: Int,
    onDelete: () -> Unit,
    onSend: () -> Unit,
    hasWaitingAssistant: Boolean,
) {
    val player = rememberChatAudioPlayer()
    val playerState by player.state.collectAsState()

    LaunchedEffect(attachment.filePath) {
        player.load(attachment.filePath)
    }

    val isPlaying = playerState is AudioPlayerState.Playing
    val positionMs: Long =
        when (val state = playerState) {
            is AudioPlayerState.Playing -> state.positionMs
            is AudioPlayerState.Paused -> state.positionMs
            else -> 0
        }
    val durationMs: Long =
        when (val state = playerState) {
            is AudioPlayerState.Ready -> state.durationMs
            is AudioPlayerState.Playing -> state.durationMs
            is AudioPlayerState.Paused -> state.durationMs
            else -> durationSeconds.toLong() * MS_PER_SECOND
        }
    val remainingMs = (durationMs - positionMs).coerceAtLeast(0)

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
                ).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeleteButton(
                onClick = {
                    player.stop()
                    onDelete()
                },
            )
            PlayPauseButton(isPlaying = isPlaying, onClick = player::playPause)
            Text(
                text = formatElapsed(if (isPlaying) positionMs else remainingMs),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
            )
        }
        SendChipButton(
            enabled = !hasWaitingAssistant,
            onClick = {
                player.stop()
                onSend()
            },
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .background(color = YralColors.Pink300, shape = CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter =
                painterResource(
                    if (isPlaying) {
                        Res.drawable.ic_audio_pause
                    } else {
                        Res.drawable.ic_audio_play
                    },
                ),
            contentDescription = if (isPlaying) "Pause recorded audio" else "Play recorded audio",
            modifier = Modifier.size(20.dp),
            tint = YralColors.Neutral0,
        )
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .background(color = Color.Transparent, shape = CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "×",
            color = YralColors.NeutralTextSecondary,
            style = LocalAppTopography.current.xlSemiBold,
        )
    }
}

@Composable
private fun SendChipButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (enabled) YralColors.Pink300 else YralColors.Neutral700
    Box(
        modifier =
            Modifier
                .background(color = bg, shape = RoundedCornerShape(20.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Send",
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.Neutral0,
        )
    }
}
