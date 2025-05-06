package com.yral.android.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.home.FeedScreenConstants.MAX_LINES_FOR_POST_DESCRIPTION
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase.Companion.INITIAL_REQUEST
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.PRE_FETCH_BEFORE_LAST
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Suppress("LongMethod")
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    // Keep track of the last feed size to detect when new items are loaded
    var lastFeedSize by remember { mutableIntStateOf(0) }
    // Track when a load was triggered to prevent multiple calls
    var loadTriggered by remember { mutableStateOf(false) }
    // Track if new items have been loaded since loading started
    var newItemsAddedSinceLoading by remember { mutableStateOf(false) }

    // Function to determine if we should load more content
    val shouldLoadMore =
        remember(
            state.currentPageOfFeed,
            state.feedDetails.size,
            state.isLoadingMore,
            loadTriggered,
        ) {
            val hasSufficientItems = state.feedDetails.size >= INITIAL_REQUEST
            val isValidPage = state.currentPageOfFeed > 0
            val isCloseToEnd =
                state.feedDetails.isNotEmpty() &&
                    (state.feedDetails.size - state.currentPageOfFeed) <= PRE_FETCH_BEFORE_LAST
            isValidPage && hasSufficientItems && isCloseToEnd && !state.isLoadingMore && !loadTriggered
        }

    // Reset load triggered when feed size changes (meaning new data arrived)
    LaunchedEffect(state.feedDetails.size) {
        if (state.feedDetails.size > lastFeedSize) {
            // New items have been added
            loadTriggered = false
            lastFeedSize = state.feedDetails.size
            // Mark that we've received new items since loading started
            if (state.isLoadingMore) {
                newItemsAddedSinceLoading = true
            }
        }
    }

    // Reset states when loading state changes
    LaunchedEffect(state.isLoadingMore) {
        if (state.isLoadingMore) {
            // Reset flag when loading starts
            newItemsAddedSinceLoading = false
        } else {
            // Reset load triggered when loading completes
            loadTriggered = false
        }
    }

    // Handle loading more when currentPage changes
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadTriggered = true
            coroutineScope.launch {
                viewModel.loadMoreFeed()
            }
        }
    }

    // Determine if we should show the loader
    val showLoader = state.isLoadingMore && !newItemsAddedSinceLoading

    Column(modifier = modifier) {
        if (state.feedDetails.isNotEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopStart,
            ) {
                YRALReelPlayer(
                    videoUrlArray =
                        state
                            .feedDetails
                            .map {
                                Pair(
                                    it.url.toString(),
                                    it.thumbnail.toString(),
                                )
                            }.toList(),
                    initialPage = state.currentPageOfFeed,
                    onPageLoaded = { page ->
                        viewModel.onCurrentPageChange(page)
                        viewModel.setPostDescriptionExpanded(false)
                    },
                )
                UserBrief(
                    principalId = state.feedDetails[state.currentPageOfFeed].principalID,
                    profileImageUrl = state.feedDetails[state.currentPageOfFeed].profileImageURL,
                    postDescription = state.feedDetails[state.currentPageOfFeed].postDescription,
                    isPostDescriptionExpanded = state.isPostDescriptionExpanded,
                    setPostDescriptionExpanded = { viewModel.setPostDescriptionExpanded(it) },
                )
            }
            // Show loader at the bottom when loading more content AND no new items have been added yet
            if (showLoader) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    YralLoader(size = 32.dp)
                }
            }
        }
    }
}

@Composable
private fun UserBrief(
    profileImageUrl: Url?,
    principalId: String,
    postDescription: String,
    isPostDescriptionExpanded: Boolean,
    setPostDescriptionExpanded: (isExpanded: Boolean) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 22.dp,
                    end = 46.dp,
                    start = 16.dp,
                    bottom = 22.dp,
                ),
    ) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.user_brief),
            contentDescription = "image description",
            contentScale = ContentScale.FillBounds,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp,
                        bottom = 8.dp,
                        start = 8.dp,
                    ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UserBriefProfileImage(profileImageUrl)
            UserBriefDetails(
                modifier = Modifier.weight(1f),
                principalId = principalId,
                postDescription = postDescription,
                isPostDescriptionExpanded = isPostDescriptionExpanded,
                setPostDescriptionExpanded = setPostDescriptionExpanded,
            )
        }
    }
}

@Composable
private fun UserBriefProfileImage(profileImageUrl: Url?) {
    val shape = RoundedCornerShape(size = 40.dp)
    AsyncImage(
        model = profileImageUrl.toString(),
        contentDescription = "User picture",
        contentScale = ContentScale.FillBounds,
        modifier =
            Modifier
                .clip(shape)
                .border(
                    width = 2.dp,
                    color = YralColors.Pink300,
                    shape = shape,
                ).width(40.dp)
                .height(40.dp)
                .background(
                    color = YralColors.profilePicBackground,
                    shape = shape,
                ),
    )
}

@Composable
private fun UserBriefDetails(
    modifier: Modifier,
    principalId: String,
    postDescription: String,
    isPostDescriptionExpanded: Boolean,
    setPostDescriptionExpanded: (isExpanded: Boolean) -> Unit,
) {
    Column(
        modifier =
            modifier
                .clickable {
                    setPostDescriptionExpanded(!isPostDescriptionExpanded)
                },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = principalId,
            style = LocalAppTopography.current.feedCanisterId,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isPostDescriptionExpanded) {
            val scrollState = rememberScrollState()
            val maxHeight =
                LocalAppTopography
                    .current
                    .feedDescription
                    .lineHeight
                    .value * MAX_LINES_FOR_POST_DESCRIPTION
            Text(
                modifier =
                    Modifier
                        .heightIn(max = maxHeight.dp)
                        .verticalScroll(scrollState),
                text = postDescription,
                style = LocalAppTopography.current.feedDescription,
                color = Color.White,
            )
        } else {
            Text(
                text = postDescription,
                style = LocalAppTopography.current.feedDescription,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

object FeedScreenConstants {
    const val MAX_LINES_FOR_POST_DESCRIPTION = 5
}
