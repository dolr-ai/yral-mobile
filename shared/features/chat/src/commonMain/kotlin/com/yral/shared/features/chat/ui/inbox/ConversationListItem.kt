package com.yral.shared.features.chat.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val AVATAR_SIZE_DP = 54
private const val AVATAR_BORDER_DP = 2
private const val ROW_GAP_DP = 10
private const val NAME_MESSAGE_GAP_DP = 6
private const val DATE_BADGE_GAP_DP = 4
private const val BADGE_SIZE_DP = 24
private const val MAX_LAST_MESSAGE_LINES = 1

@Composable
fun ConversationListItem(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarUrl =
        conversation.conversationUser?.profilePictureUrl?.takeIf { it.isNotBlank() }
            ?: conversation.influencer.avatarUrl
    val displayName =
        conversation.conversationUser?.let { user ->
            user.username?.takeIf { it.isNotBlank() }
                ?: resolveUsername("", user.principalId)
                ?: ""
        } ?: conversation.influencer.displayName.ifBlank { conversation.influencer.name }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationAvatar(avatarUrl = avatarUrl)
        ConversationNameAndPreview(
            modifier = Modifier.weight(1f),
            displayName = displayName,
            lastMessagePreview = conversation.lastMessage?.content?.takeIf { it.isNotBlank() },
        )
        ConversationTimeAndBadge(
            timeText = conversation.lastMessage?.createdAt?.let(::formatConversationTime) ?: "",
            unreadCount = conversation.unreadCount,
            isBot = conversation.conversationUser != null,
        )
    }
}

@Composable
private fun ConversationAvatar(avatarUrl: String) {
    Box(
        modifier =
            Modifier
                .size(AVATAR_SIZE_DP.dp)
                .clip(CircleShape),
    ) {
        YralAsyncImage(
            imageUrl = avatarUrl,
            modifier = Modifier.size(AVATAR_SIZE_DP.dp),
            shape = CircleShape,
            contentScale = ContentScale.Crop,
            border = AVATAR_BORDER_DP.dp,
            borderColor = YralColors.Neutral800,
        )
    }
}

@Composable
private fun ConversationNameAndPreview(
    modifier: Modifier,
    displayName: String,
    lastMessagePreview: String?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NAME_MESSAGE_GAP_DP.dp),
    ) {
        Text(
            text = displayName,
            style = LocalAppTopography.current.mdSemiBold,
            color = YralColors.NeutralTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!lastMessagePreview.isNullOrBlank()) {
            Text(
                text = lastMessagePreview,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
                maxLines = MAX_LAST_MESSAGE_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConversationTimeAndBadge(
    timeText: String,
    unreadCount: Int,
    isBot: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DATE_BADGE_GAP_DP.dp),
    ) {
        Text(
            text = timeText,
            style = LocalAppTopography.current.regRegular,
            color = YralColors.Neutral500,
        )
        if (unreadCount > 0 && !isBot) {
            Box(
                modifier =
                    Modifier
                        .size(BADGE_SIZE_DP.dp)
                        .background(
                            color = YralColors.Pink300,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formatAbbreviation(unreadCount.toLong(), 1),
                    style = LocalAppTopography.current.regSemiBold,
                    color = YralColors.Neutral0,
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
internal fun formatConversationTime(isoTimestamp: String): String {
    val normalized =
        if (isoTimestamp.endsWith('Z') || isoTimestamp.matches(Regex(".*[+-]\\d{2}:\\d{2}$"))) {
            isoTimestamp
        } else {
            "${isoTimestamp}Z"
        }
    return runCatching {
        val instant = Instant.parse(normalized)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val h = local.hour.toString().padStart(2, '0')
        val m = local.minute.toString().padStart(2, '0')
        "$h:$m"
    }.getOrElse { "" }
}
