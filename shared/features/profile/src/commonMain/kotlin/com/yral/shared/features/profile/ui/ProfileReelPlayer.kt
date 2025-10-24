package com.yral.shared.features.profile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener
import com.yral.shared.reportVideo.domain.models.ReportSheetState
import com.yral.shared.reportVideo.domain.models.ReportVideoData
import com.yral.shared.reportVideo.ui.ReportVideo
import com.yral.shared.reportVideo.ui.ReportVideoSheet
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.profile.generated.resources.Res
import yral_mobile.shared.features.profile.generated.resources.deleting
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.delete
import yral_mobile.shared.libs.designsystem.generated.resources.ic_share
import yral_mobile.shared.libs.designsystem.generated.resources.ic_views
import yral_mobile.shared.libs.designsystem.generated.resources.shadow
import yral_mobile.shared.libs.designsystem.generated.resources.shadow_bottom
import yral_mobile.shared.libs.designsystem.generated.resources.your_videos
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileReelPlayer(
    reelVideos: LazyPagingItems<FeedDetails>,
    initialPage: Int,
    isOwnProfile: Boolean,
    userName: String?,
    deletingVideoId: String,
    isReporting: Boolean,
    reportSheetState: ReportSheetState,
    onReportClick: (pageNo: Int, video: FeedDetails) -> Unit,
    dismissReportSheet: (video: FeedDetails) -> Unit,
    reportVideo: (pageNo: Int, video: FeedDetails, reportVideoData: ReportVideoData) -> Unit,
    onBack: () -> Unit,
    onDeleteVideo: (FeedDetails) -> Unit,
    onShareClick: (FeedDetails) -> Unit,
    onViewsClick: (FeedDetails) -> Unit,
    getPrefetchListener: (reel: Reels) -> PrefetchVideoListener,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val videoReels =
        remember(reelVideos.itemSnapshotList) {
            reelVideos.itemSnapshotList.items.map { it.toReel() }
        }
    if (videoReels.isNotEmpty()) {
        YRALReelPlayer(
            modifier = modifier.fillMaxSize(),
            reels = videoReels,
            maxReelsInPager = videoReels.size,
            initialPage = initialPage,
            onPageLoaded = { },
            recordTime = { _, _ -> },
            didVideoEnd = { },
            getPrefetchListener = getPrefetchListener,
            getVideoListener = { null },
        ) { pageNo ->
            val currentVideo = reelVideos[pageNo]
            if (currentVideo != null) {
                ProfileReelOverlay(
                    currentVideo = currentVideo,
                    isOwnProfile = isOwnProfile,
                    userName = userName,
                    isDeleting = deletingVideoId == currentVideo.videoID,
                    onBack = onBack,
                    onReportClick = { onReportClick(pageNo, currentVideo) },
                    onDeleteVideo = { onDeleteVideo(currentVideo) },
                    onShareClick = { onShareClick(currentVideo) },
                    onViewsClick = { onViewsClick(currentVideo) },
                )
                when (val reportSheetState = reportSheetState) {
                    ReportSheetState.Closed -> Unit
                    is ReportSheetState.Open -> {
                        ReportVideoSheet(
                            onDismissRequest = { dismissReportSheet(currentVideo) },
                            bottomSheetState = bottomSheetState,
                            isLoading = isReporting,
                            reasons = reportSheetState.reasons,
                            onSubmit = { reportVideoData ->
                                reportVideo(pageNo, currentVideo, reportVideoData)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileReelOverlay(
    currentVideo: FeedDetails,
    isOwnProfile: Boolean,
    userName: String?,
    isDeleting: Boolean,
    onBack: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteVideo: () -> Unit,
    onShareClick: () -> Unit,
    onViewsClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Header(
            modifier = Modifier.align(Alignment.TopStart),
            isOwnProfile = isOwnProfile,
            userName = userName,
            onBack = onBack,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .paint(
                        painter = painterResource(DesignRes.drawable.shadow_bottom),
                        contentScale = ContentScale.FillBounds,
                    ),
        ) {
            Caption(
                modifier = Modifier.align(Alignment.BottomStart),
                caption = currentVideo.postDescription,
            )
            ActionsRight(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 89.dp),
                views = currentVideo.viewCount.toLong(),
                isOwnProfile = isOwnProfile,
                onReportClick = onReportClick,
                onShareClick = onShareClick,
                onDeleteVideo = onDeleteVideo,
                onViewsClick = onViewsClick,
            )
        }
        DeletingOverlay(
            isDeleting = isDeleting,
            loaderSize = 54.dp,
            textStyle = LocalAppTopography.current.lgMedium,
        )
    }
}

@Composable
private fun Header(
    modifier: Modifier,
    isOwnProfile: Boolean,
    userName: String?,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(DesignRes.drawable.shadow),
                    contentScale = ContentScale.FillBounds,
                ).padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            modifier = Modifier.size(24.dp).clickable { onBack() },
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "back",
        )
        Text(
            text =
                if (isOwnProfile) {
                    stringResource(DesignRes.string.your_videos)
                } else {
                    userName ?: ""
                },
            style = LocalAppTopography.current.xlBold,
            color = Color.White,
        )
    }
}

@Composable
private fun Caption(
    modifier: Modifier,
    caption: String,
) {
    var isPostDescriptionExpanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            modifier.fillMaxWidth().padding(start = 16.dp, end = 61.dp, bottom = 28.dp).clickable {
                isPostDescriptionExpanded =
                    !isPostDescriptionExpanded
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPostDescriptionExpanded) {
            val scrollState = rememberScrollState()
            val maxHeight =
                LocalAppTopography.current.feedDescription.lineHeight.value *
                    ProfileMainScreenConstants.MAX_LINES_FOR_POST_DESCRIPTION
            Text(
                modifier = Modifier.heightIn(max = maxHeight.dp).verticalScroll(scrollState),
                text = caption,
                style = LocalAppTopography.current.feedDescription,
                color = Color.White,
            )
        } else {
            Text(
                text = caption,
                style = LocalAppTopography.current.feedDescription,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionsRight(
    modifier: Modifier,
    views: Long,
    isOwnProfile: Boolean,
    onReportClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteVideo: () -> Unit,
    onViewsClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShareIcon(
            onClick = onShareClick,
        )

        ViewsIcon(
            views = views,
            onViewsClick = onViewsClick,
        )

        ReportVideo(
            onReportClicked = onReportClick,
        )

        if (isOwnProfile) {
            DeleteIcon(
                onDeleteVideo = onDeleteVideo,
            )
        }
    }
}

@Composable
private fun ViewsIcon(
    modifier: Modifier = Modifier,
    views: Long,
    onViewsClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onViewsClick() },
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.ic_views),
            contentDescription = "video views",
            contentScale = ContentScale.None,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = formatAbbreviation(views, 1),
            style = LocalAppTopography.current.regSemiBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun DeleteIcon(
    modifier: Modifier = Modifier,
    onDeleteVideo: () -> Unit,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(DesignRes.drawable.delete),
            contentDescription = "delete video",
            modifier = Modifier.size(36.dp).clickable { onDeleteVideo() },
        )
    }
}

@Composable
private fun ShareIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Image(
        modifier =
            modifier
                .size(36.dp)
                .padding(1.5.dp)
                .clickable(onClick = onClick),
        painter = painterResource(DesignRes.drawable.ic_share),
        contentDescription = "share video",
        contentScale = ContentScale.None,
    )
}

@Composable
private fun DeletingOverlay(
    isDeleting: Boolean,
    loaderSize: Dp = 24.dp,
    textStyle: TextStyle = LocalAppTopography.current.baseMedium,
) {
    AnimatedVisibility(
        visible = isDeleting,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(YralColors.ScrimColor).clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                YralLoader(size = loaderSize, LottieRes.READ_LOADER)
                Text(
                    text = stringResource(Res.string.deleting),
                    style = textStyle,
                    color = YralColors.NeutralTextPrimary,
                )
            }
        }
    }
}

private fun FeedDetails.toReel() =
    Reels(
        videoUrl = url,
        thumbnailUrl = thumbnail,
        videoId = videoID,
    )
