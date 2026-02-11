package com.yral.shared.libs.videoplayback.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.PlaybackCoordinator
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun VideoFeed(
    items: List<MediaDescriptor>,
    coordinator: PlaybackCoordinator,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    releaseOnDispose: Boolean = false,
    overlay: @Composable (index: Int, item: MediaDescriptor) -> Unit = { _, _ -> },
) {
    if (items.isEmpty()) {
        return
    }
    val pagerState = rememberPagerState(pageCount = { items.size })
    VideoFeedSync(items = items, coordinator = coordinator)
    VideoPagerEffects(
        pagerState = pagerState,
        itemsCount = items.size,
        coordinator = coordinator,
    )
    VideoReleaseEffect(
        coordinator = coordinator,
        releaseOnDispose = releaseOnDispose,
    )

    VerticalPager(
        state = pagerState,
        modifier = modifier,
        contentPadding = contentPadding,
        beyondViewportPageCount = 1,
        key = { index -> items[index].id },
    ) { page ->
        VideoSurfaceSlot(
            index = page,
            coordinator = coordinator,
            modifier = Modifier.fillMaxSize(),
            overlay = { overlay(page, items[page]) },
        )
    }
}

@Composable
fun VideoFeedSync(
    items: List<MediaDescriptor>,
    coordinator: PlaybackCoordinator,
) {
    var previousItems by remember { mutableStateOf<List<MediaDescriptor>>(emptyList()) }
    LaunchedEffect(items, coordinator) {
        if (previousItems.isNotEmpty()) {
            previousItems = emptyList()
        }
        val previous = previousItems
        if (previous.isEmpty()) {
            coordinator.setFeed(items)
        } else if (isAppend(previous, items)) {
            val appended = items.drop(previous.size)
            if (appended.isNotEmpty()) {
                coordinator.appendFeed(appended)
            }
        } else {
            coordinator.setFeed(items)
        }
        previousItems = items
    }
}

@Composable
fun VideoReleaseEffect(
    coordinator: PlaybackCoordinator,
    releaseOnDispose: Boolean,
) {
    val releaseOnDisposeState = rememberUpdatedState(releaseOnDispose)
    DisposableEffect(coordinator) {
        onDispose {
            if (releaseOnDisposeState.value) {
                coordinator.release()
            }
        }
    }
}

@Composable
fun VideoPagerEffects(
    pagerState: PagerState,
    itemsCount: Int,
    coordinator: PlaybackCoordinator,
    scrollHintThreshold: Float = 0.15f,
) {
    LaunchedEffect(pagerState, itemsCount, coordinator) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                if (page in 0 until itemsCount) {
                    coordinator.setActiveIndex(page)
                }
            }
    }

    LaunchedEffect(pagerState, itemsCount, scrollHintThreshold, coordinator) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .map { (page, offset) ->
                predictedIndexFromOffset(
                    currentIndex = page,
                    offsetFraction = offset,
                    threshold = scrollHintThreshold,
                )
            }.distinctUntilChanged()
            .collectLatest { predicted ->
                if (predicted != null && predicted in 0 until itemsCount) {
                    coordinator.setScrollHint(predictedIndex = predicted, velocity = null)
                }
            }
    }
}

fun predictedIndexFromOffset(
    currentIndex: Int,
    offsetFraction: Float,
    threshold: Float = 0.15f,
): Int? =
    when {
        offsetFraction > threshold -> currentIndex + 1
        offsetFraction < -threshold -> currentIndex - 1
        else -> null
    }

@Suppress("UnusedParameter")
@Composable
fun VideoSurfaceSlot(
    index: Int,
    coordinator: PlaybackCoordinator,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    surfaceType: VideoSurfaceType = VideoSurfaceType.SurfaceView,
    shutter: @Composable () -> Unit = {},
    overlay: @Composable () -> Unit = {},
) {
    Box(modifier = modifier) {
        var surfaceHandle by remember { mutableStateOf<VideoSurfaceHandle?>(null) }

        VideoSurface(
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            shutter = shutter,
            surfaceType = surfaceType,
            onHandleReady = { handle -> surfaceHandle = handle },
        )

        DisposableEffect(surfaceHandle, index, coordinator) {
            val handle = surfaceHandle
            var bound = false
            if (handle != null) {
                coordinator.bindSurface(index, handle)
                bound = true
            }
            onDispose {
                if (bound) {
                    handle?.let { coordinator.unbindSurface(index, it.id) }
                }
            }
        }

        overlay()
    }
}

@Suppress("ReturnCount")
private fun isAppend(
    previous: List<MediaDescriptor>,
    current: List<MediaDescriptor>,
): Boolean {
    if (current.size < previous.size) return false
    for (index in previous.indices) {
        if (previous[index].id != current[index].id) return false
    }
    return true
}
