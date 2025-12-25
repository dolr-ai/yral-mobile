package com.yral.shared.features.feed.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.analytics.events.FeedType
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.OverlayType
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralLoadingDots
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.reportVideo.ui.ReportVideo
import com.yral.shared.rust.service.domain.models.toCanisterData
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.feed.generated.resources.ic_ai_feed
import yral_mobile.shared.features.feed.generated.resources.ic_normal_video
import yral_mobile.shared.libs.designsystem.generated.resources.ic_follow
import yral_mobile.shared.libs.designsystem.generated.resources.ic_following
import yral_mobile.shared.libs.designsystem.generated.resources.ic_share
import yral_mobile.shared.libs.designsystem.generated.resources.ic_views
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.pink_gradient_background
import kotlin.time.Duration.Companion.seconds
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun ColumnScope.FeedActionsRight(
    pageNo: Int,
    state: FeedState,
    feedViewModel: FeedViewModel,
    openProfile: (userCanisterData: CanisterData) -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f).padding(top = 65.dp, end = 10.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        if (state.availableFeedTypes.size > 1) {
            FeedToggle(
                feedType = state.feedType,
                isLoadingMore = state.isLoadingMore,
                onSelectFeed = { feedViewModel.updateFeedType(it) },
                pushFeedToggleClicked = { type, isExpanded ->
                    feedViewModel.pushFeedToggleClicked(type, isExpanded)
                },
                modifier = Modifier.offset(y = 16.dp),
            )
        }
    }
    val feedDetails = state.feedDetails[pageNo]
    if (state.overlayType in listOf(OverlayType.GAME_TOGGLE, OverlayType.DAILY_RANK)) {
        feedDetails.profileImageURL?.let { profileImage ->
            Column(modifier = Modifier.offset(y = 16.dp)) {
                YralAsyncImage(
                    imageUrl = profileImage,
                    border = 2.dp,
                    borderColor = Color.White,
                    backgroundColor = YralColors.ProfilePicBackground,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clickable { openProfile(feedDetails.toCanisterData()) },
                )
                Image(
                    painter =
                        painterResource(
                            resource =
                                if (feedDetails.isFollowing) {
                                    DesignRes.drawable.ic_following
                                } else {
                                    DesignRes.drawable.ic_follow
                                },
                        ),
                    contentDescription = "follow",
                    contentScale = ContentScale.None,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .offset(y = (-10).dp)
                            .clickable {
                                if (feedDetails.isFollowing) {
                                    openProfile(feedDetails.toCanisterData())
                                } else {
                                    if (state.isLoggedIn) {
                                        feedViewModel.follow(feedDetails.toCanisterData())
                                    } else {
                                        feedViewModel.pushFollowClicked(feedDetails.principalID)
                                        openProfile(feedDetails.toCanisterData())
                                    }
                                }
                            },
                )
            }
        }
    }
    val msgFeedVideoShare = stringResource(DesignRes.string.msg_feed_video_share)
    val msgFeedVideoShareDesc = stringResource(DesignRes.string.msg_feed_video_share_desc)
    Image(
        modifier =
            Modifier
                .size(36.dp)
                .padding(1.5.dp)
                .clickable {
                    feedViewModel.onShareClicked(
                        feedDetails,
                        msgFeedVideoShare,
                        msgFeedVideoShareDesc
                    )
                },
        painter = painterResource(DesignRes.drawable.ic_share),
        contentDescription = "share video",
        contentScale = ContentScale.None,
    )

    Column(
        modifier = Modifier.height(56.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.ic_views),
            contentDescription = "video views",
            contentScale = ContentScale.None,
            modifier =
                Modifier
                    .size(36.dp)
                    .padding(1.5.dp),
        )
        feedDetails.bulkViewCount?.let { viewCount ->
            Text(
                text = formatAbbreviation(viewCount.toLong(), 1),
                style = LocalAppTopography.current.regSemiBold,
                color = YralColors.NeutralTextPrimary,
            )
        } ?: YralLoadingDots()
    }

    ReportVideo(
        onReportClicked = { feedViewModel.toggleReportSheet(true, pageNo) },
    )
}

@Composable
private fun FeedToggle(
    feedType: FeedType,
    isLoadingMore: Boolean,
    modifier: Modifier = Modifier,
    onSelectFeed: (feedType: FeedType) -> Unit,
    pushFeedToggleClicked: (feedType: FeedType, isExpanded: Boolean) -> Unit,
    feedToggleBGOpacity: Float = 0.6f,
) {
    val icons =
        listOf(
            FeedType.AI to yral_mobile.shared.features.feed.generated.resources.Res.drawable.ic_ai_feed,
            FeedType.DEFAULT to yral_mobile.shared.features.feed.generated.resources.Res.drawable.ic_normal_video,
        )
    var isExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(3.seconds)
            isExpanded = false
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.5.dp, Alignment.Top),
        horizontalAlignment = Alignment.End,
        modifier =
            modifier
                .background(
                    color = YralColors.Neutral800.copy(feedToggleBGOpacity),
                    shape = CircleShape,
                ).padding(4.5.dp)
                .alpha(if (isLoadingMore) 1 / 2f else 1f),
    ) {
        if (isExpanded) {
            icons.forEach { (type, drawable) ->
                FeedIcon(
                    drawable = drawable,
                    isSelected = feedType == type,
                    onSelectFeed = {
                        isExpanded = false
                        pushFeedToggleClicked(type, true)
                        onSelectFeed(type)
                    },
                )
            }
        } else {
            icons.find { (type, _) -> feedType == type }?.let { (type, drawable) ->
                FeedIcon(
                    drawable = drawable,
                    isSelected = true,
                    onSelectFeed = {
                        if (!isLoadingMore) {
                            isExpanded = true
                            pushFeedToggleClicked(type, false)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FeedIcon(
    drawable: DrawableResource,
    isSelected: Boolean,
    onSelectFeed: () -> Unit,
) {
    val background =
        if (isSelected) {
            Modifier
                .clip(CircleShape)
                .paint(
                    painter = painterResource(DesignRes.drawable.pink_gradient_background),
                    contentScale = ContentScale.FillBounds,
                )
        } else {
            Modifier
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(2.45.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier =
            Modifier
                .width(32.dp)
                .height(32.dp)
                .then(background)
                .padding(6.dp)
                .clickable { onSelectFeed() },
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = "feed",
            contentScale = ContentScale.Inside,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
