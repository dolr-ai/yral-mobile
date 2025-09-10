package com.yral.android.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import com.yral.shared.libs.videoPlayer.model.Reels

@Composable
fun ProfileReelPlayer(
    reelVideos: LazyPagingItems<FeedDetails>,
    initialPage: Int,
    deletingVideoId: String,
    onBack: () -> Unit,
    onDeleteVideo: (FeedDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            getPrefetchListener = { reel -> PrefetchVideoListenerImpl(reel) },
            getVideoListener = { null },
        ) { pageNo ->
            val currentVideo = reelVideos[pageNo]
            if (currentVideo != null) {
                ProfileReelOverlay(
                    currentVideo = currentVideo,
                    isDeleting = deletingVideoId == currentVideo.videoID,
                    onBack = onBack,
                    onDeleteVideo = { onDeleteVideo(currentVideo) },
                )
            }
        }
    }
}

@Composable
private fun ProfileReelOverlay(
    currentVideo: FeedDetails,
    isDeleting: Boolean,
    onBack: () -> Unit,
    onDeleteVideo: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Header(
            modifier = Modifier.align(Alignment.TopStart),
            onBack = onBack,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .paint(
                        painter = painterResource(R.drawable.shadow_bottom),
                        contentScale = ContentScale.FillBounds,
                    ),
        ) {
            Caption(
                modifier = Modifier.align(Alignment.BottomStart),
                caption = currentVideo.postDescription,
            )
            DeleteIcon(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 89.dp),
                onDeleteVideo = onDeleteVideo,
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
    onBack: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(R.drawable.shadow),
                    contentScale = ContentScale.FillBounds,
                ).padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            modifier = Modifier.size(24.dp).clickable { onBack() },
            painter = painterResource(id = R.drawable.arrow_left),
            contentDescription = "back",
        )
        Text(
            text = stringResource(R.string.your_videos),
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
                    ProfileScreenConstants.MAX_LINES_FOR_POST_DESCRIPTION
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
private fun DeleteIcon(
    modifier: Modifier,
    onDeleteVideo: () -> Unit,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.delete),
            contentDescription = "delete video",
            modifier = Modifier.size(36.dp).clickable { onDeleteVideo() },
        )
    }
}

@Composable
private fun DeletingOverlay(
    isDeleting: Boolean,
    loaderSize: Dp = 24.dp,
    textStyle: TextStyle = LocalAppTopography.current.baseMedium,
) {
    AnimatedVisibility(
        visible = isDeleting,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(YralColors.ScrimColor).clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                YralLoader(size = loaderSize, R.raw.read_loader)
                Text(
                    text = stringResource(R.string.deleting),
                    style = textStyle,
                    color = YralColors.NeutralTextPrimary,
                )
            }
        }
    }
}

private fun FeedDetails.toReel() =
    Reels(
        videoUrl = url.toString(),
        thumbnailUrl = thumbnail.toString(),
        videoId = videoID,
    )
