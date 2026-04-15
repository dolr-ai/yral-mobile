package com.yral.shared.app.ui.screens.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.app.ui.ChatUnreadBadge
import com.yral.shared.app.ui.chatUnreadBadgeText
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.ui.FeedActionsRight
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.ui.FeedStreakChip
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.inbox

@Composable
fun FeedScaffoldScreen(
    component: FeedComponent,
    feedViewModel: FeedViewModel,
    chatUnreadCount: Int,
    onInboxClick: () -> Unit,
    onDailyStreakClick: (Long) -> Unit,
) {
    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val badgeText = chatUnreadBadgeText(chatUnreadCount)

    FeedScreen(
        component = component,
        viewModel = feedViewModel,
        topOverlay = { _ ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            ) {
                FeedStreakChip(
                    streakCount = feedState.streakCount,
                    modifier = Modifier.align(Alignment.TopStart),
                    onClick =
                        feedState.streakCount?.let { streakCount ->
                            { onDailyStreakClick(streakCount) }
                        },
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    Image(
                        painter = painterResource(Res.drawable.inbox),
                        contentDescription = "Inbox",
                        modifier =
                            Modifier
                                .size(38.dp)
                                .clickable { onInboxClick() },
                    )
                    badgeText?.let { unreadBadgeText ->
                        ChatUnreadBadge(
                            text = unreadBadgeText,
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                }
            }
        },
        bottomOverlay = { _, _ -> },
        actionsRight = { pageNo ->
            FeedActionsRight(
                pageNo = pageNo,
                state = feedState,
                feedViewModel = feedViewModel,
                openProfile = component::openProfile,
            )
        },
        onPageChanged = { _, _ -> },
        onEdgeScrollAttempt = { },
        limitReelCount = feedState.feedDetails.size,
    )
}
