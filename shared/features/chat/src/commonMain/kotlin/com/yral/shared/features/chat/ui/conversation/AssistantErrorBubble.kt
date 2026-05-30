package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.domain.models.AssistantError
import com.yral.shared.features.chat.domain.models.AssistantErrorCode
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

/**
 * Inline error bubble rendered in the assistant's slot when an SSE `error`
 * event fires (Phase 4 plumbing → Phase 6 UI) or when the non-streaming
 * send returns an [AssistantError] (Phase 9 carve-outs).
 *
 * Phase 6 v1 keeps the retry affordance INSIDE this bubble (not on the
 * user's preceding message). The spec §4.3 alternative — retry attached
 * to the user's bubble — is a follow-up; bundling retry into the error
 * surface keeps the visible affordance adjacent to the explanatory text
 * and avoids the cross-bubble state coupling.
 */
@Composable
internal fun AssistantErrorBubble(
    error: AssistantError,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val typography = LocalAppTopography.current
    val textColor = Color.White.copy(alpha = TEXT_ALPHA)

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        color = YralColors.Neutral900.copy(alpha = BACKGROUND_ALPHA),
                        shape = RoundedCornerShape(16.dp),
                    ).padding(PaddingValues(horizontal = 12.dp, vertical = 10.dp)),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = WARNING_ICON,
                    style = typography.baseRegular,
                    color = textColor,
                )
                Text(
                    text = error.message,
                    style = typography.baseRegular.copy(fontStyle = FontStyle.Italic),
                    color = textColor,
                )
            }
            when {
                error.code == AssistantErrorCode.BLOCKED_CONTENT -> {
                    Text(
                        text = REPHRASE_HINT,
                        style = typography.smRegular,
                        color = textColor.copy(alpha = HINT_ALPHA),
                    )
                }

                error.retryable && onRetry != null -> {
                    Text(
                        text = RETRY_LABEL,
                        style = typography.smSemiBold,
                        color = YralColors.BlueTextPrimary,
                        modifier = Modifier.clickable { onRetry() },
                    )
                }
            }
        }
    }
}

private const val WARNING_ICON = "⚠"
private const val REPHRASE_HINT = "Try rephrasing your message"
private const val RETRY_LABEL = "↻ Try again"
private const val TEXT_ALPHA = 0.85f
private const val BACKGROUND_ALPHA = 0.5f
private const val HINT_ALPHA = 0.7f
