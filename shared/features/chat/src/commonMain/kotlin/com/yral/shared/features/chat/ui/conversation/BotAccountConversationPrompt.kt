@file:Suppress("MagicNumber")

package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

private val BOT_ACCOUNT_PROMPT_CORNER_RADIUS = 24.dp
private val BOT_ACCOUNT_PROMPT_OUTER_PADDING = 16.dp
private val BOT_ACCOUNT_PROMPT_CONTENT_PADDING = 20.dp
private val BOT_ACCOUNT_PROMPT_AVATAR_SIZE = 68.dp
private val BOT_ACCOUNT_PROMPT_AVATAR_INNER_SIZE = 62.dp
private val BOT_ACCOUNT_PROMPT_BUTTON_HEIGHT = 48.dp

private val BOT_ACCOUNT_PROMPT_SURFACE = Color(0xFF120910)
private val BOT_ACCOUNT_PROMPT_BORDER_START = YralColors.ProGradientOrange.copy(alpha = 0.85f)
private val BOT_ACCOUNT_PROMPT_BORDER_END = YralColors.ProGradientPink.copy(alpha = 0.95f)
private val BOT_ACCOUNT_PROMPT_AVATAR_RING_START = Color(0xFFFFC170)
private val BOT_ACCOUNT_PROMPT_AVATAR_RING_END = Color(0xFFFF4DA3)

@Composable
internal fun BotAccountConversationPrompt(
    message: String,
    buttonText: String,
    avatarUrl: String?,
    isSwitching: Boolean,
    onSwitchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = BOT_ACCOUNT_PROMPT_OUTER_PADDING)
                .clip(RoundedCornerShape(BOT_ACCOUNT_PROMPT_CORNER_RADIUS))
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    BOT_ACCOUNT_PROMPT_BORDER_START,
                                    BOT_ACCOUNT_PROMPT_BORDER_END,
                                ),
                            start = Offset.Zero,
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                        ),
                ).padding(1.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BOT_ACCOUNT_PROMPT_CORNER_RADIUS))
                    .background(BOT_ACCOUNT_PROMPT_SURFACE)
                    .padding(BOT_ACCOUNT_PROMPT_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            avatarUrl?.takeIf { it.isNotBlank() }?.let { influencerAvatarUrl ->
                GradientAvatar(imageUrl = influencerAvatarUrl)
            }

            Text(
                text = message,
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            YralGradientButton(
                text = buttonText,
                onClick = onSwitchClick,
                modifier = Modifier.fillMaxWidth(),
                buttonState =
                    if (isSwitching) {
                        YralButtonState.Loading
                    } else {
                        YralButtonState.Enabled
                    },
                buttonHeight = BOT_ACCOUNT_PROMPT_BUTTON_HEIGHT,
            )
        }
    }
}

@Composable
private fun GradientAvatar(imageUrl: String) {
    Box(
        modifier =
            Modifier
                .size(BOT_ACCOUNT_PROMPT_AVATAR_SIZE)
                .clip(CircleShape)
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    BOT_ACCOUNT_PROMPT_AVATAR_RING_START,
                                    BOT_ACCOUNT_PROMPT_AVATAR_RING_END,
                                ),
                        ),
                ).padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        YralAsyncImage(
            imageUrl = imageUrl,
            modifier = Modifier.size(BOT_ACCOUNT_PROMPT_AVATAR_INNER_SIZE),
            contentScale = ContentScale.Crop,
            shape = CircleShape,
        )
    }
}
