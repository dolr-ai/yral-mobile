package com.yral.android.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase.Companion.INITIAL_REQUEST
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // Keep track of the last feed size to detect when new items are loaded
    var lastFeedSize by remember { mutableIntStateOf(0) }
    // Track when a load was triggered to prevent multiple calls
    var loadTriggered by remember { mutableStateOf(false) }
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
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
    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        items(feedDetails) { item ->
            FeedItem(item)
        }
        if (isLoadingMore) {
            item {
                YralLoader()
            }
        }
    }
}

@Composable
private fun FeedItem(item: FeedDetails) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ).padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.canisterID,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = item.url.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
