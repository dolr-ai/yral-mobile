package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.util.EdgeScrollDetectConnection
import com.yral.shared.libs.videoPlayer.util.ReelScrollDirection
import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.PlaybackEventReporter
import com.yral.shared.libs.videoplayback.ui.VideoFeedSync
import com.yral.shared.libs.videoplayback.ui.VideoPagerEffects
import com.yral.shared.libs.videoplayback.ui.VideoSurfaceSlot
import com.yral.shared.libs.videoplayback.ui.rememberPlaybackCoordinatorWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged

@Suppress("LongMethod")
@Composable
fun YRALReelPlayer(
    modifier: Modifier = Modifier,
    reels: List<Reels>,
    maxReelsInPager: Int,
    initialPage: Int,
    onPageLoaded: (currentPage: Int) -> Unit,
    recordTime: (Int, Int) -> Unit,
    didVideoEnd: () -> Unit,
    onEdgeScrollAttempt: (pageNo: Int, atStart: Boolean, direction: ReelScrollDirection) -> Unit = { _, _, _ -> },
    overlayContent: @Composable (pageNo: Int, scrollToNext: () -> Unit) -> Unit,
) {
    val pageCount = minOf(reels.size, maxReelsInPager)
    if (pageCount == 0) return

    val visibleReels = remember(reels, pageCount) { reels.take(pageCount) }
    val mediaItems =
        remember(visibleReels) {
            visibleReels.map { reel ->
                MediaDescriptor(
                    id = reel.videoId,
                    uri = reel.videoUrl,
                )
            }
        }

    val pagerState =
        rememberPagerState(
            pageCount = { pageCount },
            initialPage = initialPage.coerceAtMost(pageCount - 1),
        )

    LaunchedEffect(pagerState, visibleReels) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onPageLoaded(page)
            }
    }

    val reporter =
        rememberPlaybackEventReporter(
            didVideoEnd = didVideoEnd,
            recordTime = recordTime,
        )
    val coordinator =
        rememberPlaybackCoordinatorWithLifecycle(
            deps = CoordinatorDeps(reporter = reporter),
        )
    VideoFeedSync(items = mediaItems, coordinator = coordinator)
    VideoPagerEffects(
        pagerState = pagerState,
        itemsCount = mediaItems.size,
        coordinator = coordinator,
    )
    val edgeDetectConnection =
        remember(pageCount) {
            EdgeScrollDetectConnection(
                pageCount = pageCount,
                pagerState = pagerState,
                isVertical = true,
                onEdgeScrollAttempt = onEdgeScrollAttempt,
            )
        }

    var autoScrollToNext by remember { mutableStateOf(false) }
    LaunchedEffect(autoScrollToNext) {
        if (autoScrollToNext) {
            if (pagerState.currentPage < pageCount - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
            autoScrollToNext = false
        }
    }

    VerticalPager(
        modifier = modifier.fillMaxSize().nestedScroll(edgeDetectConnection),
        state = pagerState,
        beyondViewportPageCount = 1,
        key = { page -> visibleReels.getOrNull(page)?.videoId ?: page },
    ) { page ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
        ) {
            val reel = visibleReels.getOrNull(page)
            VideoSurfaceSlot(
                index = page,
                coordinator = coordinator,
                modifier = Modifier.fillMaxSize(),
                shutter = {
                    if (reel != null) {
                        AsyncImage(
                            model = reel.thumbnailUrl,
                            contentDescription = "Thumbnail",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().background(Color.Black),
                        )
                    }
                },
                overlay = { overlayContent(page) { autoScrollToNext = true } },
            )
        }
    }
}

@Composable
private fun rememberPlaybackEventReporter(
    didVideoEnd: () -> Unit,
    recordTime: (Int, Int) -> Unit,
): PlaybackEventReporter =
    remember(didVideoEnd, recordTime) {
        object : PlaybackEventReporter {
            override fun playbackEnded(
                id: String,
                index: Int,
            ) {
                didVideoEnd()
            }

            override fun playbackProgress(
                id: String,
                index: Int,
                positionMs: Long,
                durationMs: Long,
            ) {
                if (positionMs >= 0 && durationMs > 0) {
                    recordTime(positionMs.toInt(), durationMs.toInt())
                }
            }

            override fun feedItemImpression(
                id: String,
                index: Int,
            ) = Unit
            override fun playStartRequest(
                id: String,
                index: Int,
                reason: String,
            ) = Unit
            override fun firstFrameRendered(
                id: String,
                index: Int,
            ) = Unit
            override fun timeToFirstFrame(
                id: String,
                index: Int,
                ms: Long,
            ) = Unit
            override fun rebufferStart(
                id: String,
                index: Int,
                reason: String,
            ) = Unit
            override fun rebufferEnd(
                id: String,
                index: Int,
                reason: String,
            ) = Unit
            override fun rebufferTotal(
                id: String,
                index: Int,
                ms: Long,
            ) = Unit
            override fun playbackError(
                id: String,
                index: Int,
                category: String,
                code: Any,
                message: String?,
            ) = Unit

            override fun preloadScheduled(
                id: String,
                index: Int,
                distance: Int,
                mode: String,
            ) = Unit

            override fun preloadCompleted(
                id: String,
                index: Int,
                bytes: Long,
                ms: Long,
                fromCache: Boolean,
            ) = Unit

            override fun preloadCanceled(
                id: String,
                index: Int,
                reason: String,
            ) = Unit
            override fun cacheHit(
                id: String,
                bytes: Long,
            ) = Unit
            override fun cacheMiss(
                id: String,
                bytes: Long,
            ) = Unit
        }
    }
