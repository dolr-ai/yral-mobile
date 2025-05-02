package com.yral.android.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase.Companion.INITIAL_REQUEST
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.PRE_FETCH_BEFORE_LAST
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
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
            Box(modifier = Modifier.weight(1f)) {
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
                    },
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
