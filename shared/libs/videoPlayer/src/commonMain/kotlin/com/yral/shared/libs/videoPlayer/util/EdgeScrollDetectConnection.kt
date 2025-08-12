package com.yral.shared.libs.videoPlayer.util

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.yral.shared.libs.videoPlayer.model.PlayerConfig

enum class ReelScrollDirection {
    Up, // Forward
    Down, // Backward
    Left,
    Right,
}

class EdgeScrollDetectConnection(
    private val pageCount: Int,
    private val pagerState: PagerState,
    private val playerConfig: PlayerConfig,
    private val onEdgeScrollAttempt: (pageNo: Int, atStart: Boolean, direction: ReelScrollDirection) -> Unit,
) : NestedScrollConnection {
    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source == NestedScrollSource.UserInput) {
            detectEdgeScroll(getDelta(available))
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        detectEdgeScroll(getDelta(available))
        return Velocity.Zero
    }

    private fun getDelta(available: Any): Float =
        when (available) {
            is Offset -> if (playerConfig.reelVerticalScrolling) available.y else available.x
            is Velocity -> if (playerConfig.reelVerticalScrolling) available.y else available.x
            else -> 0f
        }

    private fun detectEdgeScroll(delta: Float) {
        when {
            delta < 0f && isAtLastPage() -> {
                // Forward scrolling beyond last page
                val dir =
                    if (playerConfig.reelVerticalScrolling) ReelScrollDirection.Up else ReelScrollDirection.Left
                onEdgeScrollAttempt(pagerState.currentPage, false, dir)
            }

            delta > 0f && isAtFirstPage() -> {
                // Backward scrolling beyond first page
                val dir =
                    if (playerConfig.reelVerticalScrolling) ReelScrollDirection.Down else ReelScrollDirection.Right
                onEdgeScrollAttempt(pagerState.currentPage, true, dir)
            }
        }
    }

    private fun isAtFirstPage(): Boolean = pageCount == 0 || pagerState.currentPage <= 0
    private fun isAtLastPage(): Boolean = pageCount == 0 || pagerState.currentPage >= pageCount - 1
}
