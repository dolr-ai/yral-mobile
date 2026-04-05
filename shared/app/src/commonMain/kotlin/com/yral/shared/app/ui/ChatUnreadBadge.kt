package com.yral.shared.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.domain.models.formatChatUnreadBadgeCount
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

internal fun chatUnreadBadgeText(unreadCount: Int): String? = formatChatUnreadBadgeCount(unreadCount)

@Composable
internal fun ChatUnreadBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .offset(x = 12.dp, y = (-4).dp)
                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                .background(
                    color = YralColors.Pink300,
                    shape = RoundedCornerShape(100.dp),
                ).padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalAppTopography.current.smSemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}
