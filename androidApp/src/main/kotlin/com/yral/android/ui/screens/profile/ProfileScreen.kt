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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.components.DeleteConfirmationSheet
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.profile.ProfileScreenConstants.GRID_ITEM_ASPECT_RATIO
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.YralButtonType
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.core.session.AccountInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val viewModel = remember { ProfileViewModel() }
    val state by viewModel.viewState.collectAsState()
    var deleteConfirmation by remember { mutableStateOf("") }
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    Column(modifier = modifier.fillMaxSize()) {
        ProfileHeader()
        Spacer(modifier = Modifier.height(8.dp))
        state.accountInfo?.let { info ->
            AccountInfoSection(accountInfo = info)
        }
        when (val state = state.uiState) {
            is ProfileUiState.Loading -> {
                LoadingContent()
            }

            is ProfileUiState.Success -> {
                SuccessContent(
                    videos = state.videos,
                    onDeleteVideo = { videoId -> deleteConfirmation = videoId },
                )
            }

            is ProfileUiState.Error -> {
                ErrorContent(message = state.message)
            }
        }
    }
    if (deleteConfirmation.isNotEmpty()) {
        DeleteConfirmationSheet(
            bottomSheetState = bottomSheetState,
            title = stringResource(R.string.delete_video),
            subTitle = "",
            confirmationMessage = stringResource(R.string.video_will_be_deleted_permanently),
            cancelButton = stringResource(R.string.cancel),
            deleteButton = stringResource(R.string.delete),
            onDismissRequest = { deleteConfirmation = "" },
            onDelete = {
                viewModel.deleteVideo(deleteConfirmation)
                deleteConfirmation = ""
            },
        )
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

@Composable
private fun SuccessContent(
    videos: List<ProfileVideo>,
    onDeleteVideo: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        if (videos.isEmpty()) {
            EmptyStateContent()
        } else {
            VideoGridContent(
                videos = videos,
                onDeleteVideo = onDeleteVideo,
            )
        }
    }
}

@Composable
private fun EmptyStateContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
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
            onClick = {
            },
        )
    }
}

@Composable
private fun VideoGridContent(
    videos: List<ProfileVideo>,
    onDeleteVideo: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(videos) { video ->
            VideoGridItem(
                video = video,
                onDeleteClick = { onDeleteVideo(video.videoID) },
            )
        }
    }
}

@Composable
private fun VideoGridItem(
    video: ProfileVideo,
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
        Box(modifier = Modifier.fillMaxSize()) {
            // Video thumbnail
            YralAsyncImage(
                imageUrl = video.thumbnail.toString(),
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
            )
            VideoGridItemActions(
                isLiked = video.isLiked,
                likeCount = video.likeCount,
                onDeleteVideo = onDeleteClick,
            )
        }
        DeletingOverLay(video.isDeleting)
    }
}

@Composable
private fun DeletingOverLay(isDeleting: Boolean) {
    AnimatedVisibility(
        visible = isDeleting,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(YralColors.ScrimColor),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                YralLoader(size = 24.dp, R.raw.read_loader)
                Text(
                    text = stringResource(R.string.deleting),
                    style = LocalAppTopography.current.baseMedium,
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
}
