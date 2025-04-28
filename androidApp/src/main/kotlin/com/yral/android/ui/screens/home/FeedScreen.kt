package com.yral.android.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import com.yral.shared.rust.domain.models.FeedDetails
import kotlinx.coroutines.launch

private const val PRE_FETCH_BEFORE_LAST = 1

@Suppress("LongMethod")
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    feedDetails: List<FeedDetails>,
    isLoadingMore: Boolean,
    loadMoreFeed: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }
    // Keep track of the last feed size to detect when new items are loaded
    var lastFeedSize by remember { mutableIntStateOf(0) }
    // Track when a load was triggered to prevent multiple calls
    var loadTriggered by remember { mutableStateOf(false) }
    // Track if new items have been loaded since loading started
    var newItemsAddedSinceLoading by remember { mutableStateOf(false) }

    // Function to determine if we should load more content
    val shouldLoadMore =
        remember(currentPage, feedDetails.size, isLoadingMore, loadTriggered) {
            val hasSufficientItems = feedDetails.size >= INITIAL_REQUEST
            val isValidPage = currentPage > 0
            val isCloseToEnd =
                feedDetails.isNotEmpty() &&
                    (feedDetails.size - currentPage) <= PRE_FETCH_BEFORE_LAST
            isValidPage && hasSufficientItems && isCloseToEnd && !isLoadingMore && !loadTriggered
        }

    // Reset load triggered when feed size changes (meaning new data arrived)
    LaunchedEffect(feedDetails.size) {
        if (feedDetails.size > lastFeedSize) {
            // New items have been added
            loadTriggered = false
            lastFeedSize = feedDetails.size
            // Mark that we've received new items since loading started
            if (isLoadingMore) {
                newItemsAddedSinceLoading = true
            }
        }
    }

    // Reset states when loading state changes
    LaunchedEffect(isLoadingMore) {
        if (isLoadingMore) {
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
                loadMoreFeed()
            }
        }
    }

    // Determine if we should show the loader
    val showLoader = isLoadingMore && !newItemsAddedSinceLoading

    Column(modifier = modifier) {
        if (feedDetails.isNotEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                YRALReelPlayer(
                    videoUrlArray = feedDetails.map { it.url.toString() }.toList(),
                    onPageLoaded = { page ->
                        currentPage = page
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
