package com.yral.shared.features.feed.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.OverlayType
import com.yral.shared.libs.designsystem.component.YralLoadingDots
import com.yral.shared.libs.designsystem.component.features.ProfileImageView
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.reportVideo.ui.ReportVideo
import com.yral.shared.rust.service.domain.models.toCanisterData
import com.yral.shared.rust.service.utils.CanisterData
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.ic_follow
import yral_mobile.shared.libs.designsystem.generated.resources.ic_following
import yral_mobile.shared.libs.designsystem.generated.resources.ic_share
import yral_mobile.shared.libs.designsystem.generated.resources.ic_views
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_feed_video_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
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
    ) { }
    val feedDetails = state.feedDetails[pageNo]
    if (state.overlayType in listOf(OverlayType.GAME_TOGGLE, OverlayType.DAILY_RANK)) {
        feedDetails.profileImageURL?.let { profileImage ->
            Column(modifier = Modifier.offset(y = 16.dp)) {
                Box(
                    modifier =
                        Modifier
                            .clickable { openProfile(feedDetails.toCanisterData()) },
                ) {
                    if (feedDetails.isProUser) {
                        ProfileImageView(
                            imageUrl = profileImage,
                            applyFrame = true,
                            size = 36.dp,
                            frameBorderWidth = 2.dp,
                            frameBadgeSizeFraction = 0.5f,
                        )
                    } else {
                        ProfileImageView(
                            imageUrl = profileImage,
                            applyFrame = true,
                            size = 36.dp,
                            frameBrush = linearGradient(colors = listOf(Color.White, Color.White)),
                            frameBadgeSizeFraction = 0f,
                            frameBorderWidth = 2.dp,
                        )
                    }
                }
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
                        msgFeedVideoShareDesc,
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
