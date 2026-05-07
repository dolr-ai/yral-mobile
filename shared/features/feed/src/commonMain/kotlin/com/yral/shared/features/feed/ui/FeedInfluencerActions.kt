package com.yral.shared.features.feed.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yral.shared.data.domain.models.ConversationInfluencerSource
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.features.feed.generated.resources.Res
import yral_mobile.shared.features.feed.generated.resources.influencer_chat_action
import yral_mobile.shared.features.feed.generated.resources.influencer_skip_action

@Composable
fun FeedInfluencerActions(
    pageNo: Int,
    feedViewModel: FeedViewModel,
    openConversation: (OpenConversationParams) -> Unit,
    scrollToNext: () -> Unit,
) {
    val state by feedViewModel.state.collectAsState()
    val feedDetails = state.feedDetails.getOrNull(pageNo) ?: return
    if (!feedDetails.shouldShowInfluencerFeedActions()) return

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfluencerActionButton(
                contentDescription = "Skip influencer video",
                image = Res.drawable.influencer_skip_action,
                onClick = scrollToNext,
            )
            InfluencerActionButton(
                contentDescription = "Chat with influencer",
                image = Res.drawable.influencer_chat_action,
                onClick = { openConversation(feedDetails.toOpenConversationParams()) },
            )
        }
    }
}

internal fun FeedDetails.shouldShowInfluencerFeedActions(): Boolean = isAiInfluencer == true

internal fun FeedDetails.toOpenConversationParams(): OpenConversationParams =
    OpenConversationParams(
        influencerId = principalID,
        influencerSource = ConversationInfluencerSource.CARD,
        username = userName,
        displayName = displayName.takeIf { it.isNotBlank() },
        avatarUrl = profileImageURL,
    )

@Composable
private fun InfluencerActionButton(
    contentDescription: String,
    image: DrawableResource,
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(image),
        contentDescription = contentDescription,
        modifier =
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .clickable(
                    onClickLabel = contentDescription,
                    onClick = onClick,
                ),
    )
}
