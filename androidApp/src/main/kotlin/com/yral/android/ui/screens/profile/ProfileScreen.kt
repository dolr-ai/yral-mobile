package com.yral.android.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.yral.android.R
import com.yral.android.ui.components.DeleteConfirmationSheet
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.profile.ProfileScreenConstants.GRID_ITEM_ASPECT_RATIO
import com.yral.android.ui.screens.profile.ProfileScreenConstants.PULL_TO_REFRESH_INDICATOR_SIZE
import com.yral.android.ui.screens.profile.ProfileScreenConstants.PULL_TO_REFRESH_INDICATOR_THRESHOLD
import com.yral.android.ui.screens.profile.ProfileScreenConstants.PULL_TO_REFRESH_OFFSET_MULTIPLIER
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.YralButtonType
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.features.profile.viewmodel.ProfileVideo
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.features.profile.viewmodel.ViewState
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.arch.presentation.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    uploadVideo: () -> Unit,
    viewModel: ProfileViewModel = koinInstance.get(),
) {
    val state by viewModel.state.collectAsState()
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    state.openedVideo?.let { video ->
        ProfileVideoPlayer(
            modifier = modifier.fillMaxSize(),
            video = video,
            onBack = { viewModel.openVideo(null) },
            onDeleteVideo = { viewModel.confirmDelete(video.feedDetail.videoID) },
        )
    } ?: MainContent(
        modifier = modifier,
        state = state,
        uploadVideo = uploadVideo,
        onRefresh = { viewModel.onRefresh() },
        openVideo = { video -> viewModel.openVideo(video) },
        onDeleteVideo = { videoId -> viewModel.confirmDelete(videoId) },
    )

    state.deleteConfirmation?.let {
        DeleteConfirmationSheet(
            bottomSheetState = bottomSheetState,
            title = stringResource(R.string.delete_video),
            subTitle = "",
            confirmationMessage = stringResource(R.string.video_will_be_deleted_permanently),
            cancelButton = stringResource(R.string.cancel),
            deleteButton = stringResource(R.string.delete),
            onDismissRequest = { viewModel.confirmDelete(null) },
            onDelete = { viewModel.deleteVideo() },
        )
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    state: ViewState,
    uploadVideo: () -> Unit,
    onRefresh: () -> Unit,
    openVideo: (ProfileVideo) -> Unit,
    onDeleteVideo: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ProfileHeader()
        Spacer(modifier = Modifier.height(8.dp))
        state.accountInfo?.let { info ->
            AccountInfoSection(accountInfo = info)
        }
        when (val uiState = state.uiState) {
            is UiState.InProgress -> {
                LoadingContent()
            }

            is UiState.Success -> {
                SuccessContent(
                    videos = uiState.data.videos,
                    isRefreshing = state.isRefreshing,
                    uploadVideo = uploadVideo,
                    onRefresh = onRefresh,
                    openVideo = openVideo,
                    onDeleteVideo = onDeleteVideo,
                )
            }

            is UiState.Failure -> {
                ErrorContent(message = uiState.error.message ?: "")
            }

            UiState.Initial -> { }
        }
    }
}

@Composable
private fun ProfileHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.my_profile),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun AccountInfoSection(accountInfo: AccountInfo) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            YralAsyncImage(
                imageUrl = accountInfo.profilePic,
                modifier = Modifier.size(60.dp),
            )
            Text(
                text = accountInfo.userPrincipal,
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextSecondary,
            )
        }
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(YralColors.Divider),
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        YralLoader(size = 40.dp)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.something_went_wrong),
            style = LocalAppTopography.current.lgBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = LocalAppTopography.current.lgMedium,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    videos: List<ProfileVideo>,
    isRefreshing: Boolean,
    uploadVideo: () -> Unit,
    onRefresh: () -> Unit,
    openVideo: (ProfileVideo) -> Unit,
    onDeleteVideo: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        val pullRefreshState = rememberPullToRefreshState()
        val offset =
            pullRefreshState.distanceFraction *
                PULL_TO_REFRESH_INDICATOR_SIZE *
                PULL_TO_REFRESH_OFFSET_MULTIPLIER
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullRefreshState,
            indicator = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .pullToRefreshIndicator(
                                state = pullRefreshState,
                                isRefreshing = isRefreshing,
                                containerColor = Color.Transparent,
                                threshold = PULL_TO_REFRESH_INDICATOR_THRESHOLD.dp,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    YralLoader(PULL_TO_REFRESH_INDICATOR_SIZE.dp)
                }
            },
        ) {
            if (videos.isEmpty()) {
                EmptyStateContent(offset, uploadVideo)
            } else {
                VideoGridContent(
                    videos = videos,
                    offset = offset,
                    openVideo = openVideo,
                    onDeleteVideo = onDeleteVideo,
                )
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    offset: Float,
    uploadVideo: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .offset(y = offset.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.clapperboard),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.you_have_not_uploaded_any_video_yet),
            style = LocalAppTopography.current.lgMedium,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(36.dp))
        YralGradientButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Upload Video",
            buttonType = YralButtonType.White,
            onClick = uploadVideo,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoGridContent(
    videos: List<ProfileVideo>,
    offset: Float,
    openVideo: (ProfileVideo) -> Unit,
    onDeleteVideo: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .offset(y = offset.dp),
    ) {
        items(videos) { video ->
            VideoGridItem(
                video = video,
                openVideo = { openVideo(video) },
                onDeleteClick = { onDeleteVideo(video.feedDetail.videoID) },
            )
        }
    }
}

@Composable
private fun VideoGridItem(
    video: ProfileVideo,
    openVideo: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(GRID_ITEM_ASPECT_RATIO)
                .clip(shape = RoundedCornerShape(8.dp))
                .background(
                    color = YralColors.Neutral900,
                    shape = RoundedCornerShape(8.dp),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable { openVideo() },
        ) {
            // Video thumbnail
            YralAsyncImage(
                imageUrl = video.feedDetail.thumbnail.toString(),
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
            )
            VideoGridItemActions(
                isLiked = video.feedDetail.isLiked,
                likeCount = video.feedDetail.likeCount,
                onDeleteVideo = onDeleteClick,
            )
        }
        DeletingOverLay(video)
    }
}

@Composable
fun DeletingOverLay(
    video: ProfileVideo,
    loaderSize: Dp = 24.dp,
    textStyle: TextStyle = LocalAppTopography.current.baseMedium,
) {
    Logger.d("xxxx") { "isDeleting $video.isDeleting" }
    AnimatedVisibility(
        visible = video.isDeleting,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(YralColors.ScrimColor)
                    .clickable { },
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

@Composable
private fun BoxScope.VideoGridItemActions(
    isLiked: Boolean,
    likeCount: ULong,
    onDeleteVideo: () -> Unit,
) {
    // Bottom overlay with like count and delete button
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Like count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painter =
                    painterResource(
                        id =
                            if (likeCount > 0U && isLiked) {
                                R.drawable.pink_heart
                            } else {
                                R.drawable.white_heart
                            },
                    ),
                contentDescription = "like heart",
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = likeCount.toString(),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextPrimary,
            )
        }

        // Delete button
        Image(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = "Delete video",
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable { onDeleteVideo() },
        )
    }
}

object ProfileScreenConstants {
    const val GRID_ITEM_ASPECT_RATIO = 0.75f
    const val PULL_TO_REFRESH_INDICATOR_SIZE = 34f
    const val PULL_TO_REFRESH_INDICATOR_THRESHOLD = 36f
    const val PULL_TO_REFRESH_OFFSET_MULTIPLIER = 1.5f
}
