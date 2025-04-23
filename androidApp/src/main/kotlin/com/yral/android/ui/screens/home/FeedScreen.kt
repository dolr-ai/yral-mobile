package com.yral.android.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase.Companion.INITIAL_REQUEST
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import com.yral.shared.rust.domain.models.FeedDetails
import kotlinx.coroutines.launch

private const val PRE_FETCH_BEFORE_LAST = 1

@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    feedDetails: List<FeedDetails>,
    isLoadingMore: Boolean,
    loadMoreFeed: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    // Keep track of the last feed size to detect when new items are loaded
    var lastFeedSize by remember { mutableIntStateOf(0) }
    // Track when a load was triggered to prevent multiple calls
    var loadTriggered by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItemsNumber = feedDetails.size
            val lastVisibleItemIndex = currentPage
            lastVisibleItemIndex > 0 &&
                totalItemsNumber >= INITIAL_REQUEST &&
                (totalItemsNumber - lastVisibleItemIndex) <= PRE_FETCH_BEFORE_LAST &&
                feedDetails.isNotEmpty() &&
                !isLoadingMore &&
                !loadTriggered
        }
    }
    // Reset load triggered when feed size changes (meaning new data arrived)
    LaunchedEffect(feedDetails.size) {
        if (feedDetails.size > lastFeedSize) {
            loadTriggered = false
            lastFeedSize = feedDetails.size
        }
    }
    // Reset load triggered when loading state changes to not loading
    LaunchedEffect(isLoadingMore) {
        if (!isLoadingMore) {
            loadTriggered = false
        }
    }
    // Trigger load more when conditions are met
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            loadTriggered = true
            coroutineScope.launch {
                loadMoreFeed()
            }
        }
    }
    Box(modifier = modifier) {
        YRALReelPlayer(
            videoUrlArray = feedDetails.map { it.url.toString() }.toList(),
        ) { page ->
            println("HTTP Client REQUEST: current page $currentPage")
            currentPage = page
        }
    }
}
