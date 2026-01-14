package com.shortform.video.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.shortform.video.MediaDescriptor
import com.shortform.video.PlaybackCoordinator
import com.shortform.video.VideoSurfaceHandle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow

@Composable
fun ShortformVideoFeed(
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

    LaunchedEffect(items) {
        coordinator.setFeed(items)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                if (page in items.indices) {
                    coordinator.setActiveIndex(page)
                }
            }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPageOffsetFraction }
            .map { offset ->
                val current = pagerState.currentPage
                when {
                    offset > 0.15f -> current + 1
                    offset < -0.15f -> current - 1
                    else -> current
                }
            }
            .distinctUntilChanged()
            .collectLatest { predicted ->
                if (predicted in items.indices && predicted != pagerState.currentPage) {
                    coordinator.setScrollHint(predictedIndex = predicted, velocity = null)
                }
            }
    }

    DisposableEffect(releaseOnDispose) {
        onDispose {
            if (releaseOnDispose) {
                coordinator.release()
            }
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier,
        contentPadding = contentPadding,
        beyondViewportPageCount = 1,
        key = { index -> items[index].id },
    ) { page ->
        Box(modifier = Modifier.fillMaxSize()) {
            var surfaceHandle by remember { mutableStateOf<VideoSurfaceHandle?>(null) }

            VideoSurface(
                modifier = Modifier.fillMaxSize(),
                onHandleReady = { handle -> surfaceHandle = handle },
            )

            DisposableEffect(surfaceHandle) {
                val handle = surfaceHandle
                if (handle != null) {
                    coordinator.bindSurface(page, handle)
                }
                onDispose {
                    coordinator.unbindSurface(page)
                }
            }

            overlay(page, items[page])
        }
    }
}
