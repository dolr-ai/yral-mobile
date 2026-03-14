package com.yral.shared.features.feed

import com.yral.shared.features.feed.ui.FeedPaginationUtils
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.PRE_FETCH_BEFORE_LAST
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedPaginationTest {
    // ── isNearEnd ──────────────────────────────────────────────────────

    @Test
    fun isNearEndReturnsFalseWhenFeedIsEmpty() {
        assertFalse(FeedPaginationUtils.isNearEnd(feedDetailsSize = 0, currentPage = 0))
    }

    @Test
    fun isNearEndReturnsTrueWhenFeedSizeEqualsThreshold() {
        // feedDetailsSize - currentPage == PRE_FETCH_BEFORE_LAST → should trigger
        val size = PRE_FETCH_BEFORE_LAST
        assertTrue(FeedPaginationUtils.isNearEnd(feedDetailsSize = size, currentPage = 0))
    }

    @Test
    fun isNearEndReturnsTrueWhenOnLastPage() {
        assertTrue(FeedPaginationUtils.isNearEnd(feedDetailsSize = 20, currentPage = 20))
    }

    @Test
    fun isNearEndReturnsTrueWhenWithinThreshold() {
        // 25 - 20 = 5, which is <= PRE_FETCH_BEFORE_LAST (10)
        assertTrue(FeedPaginationUtils.isNearEnd(feedDetailsSize = 25, currentPage = 20))
    }

    @Test
    fun isNearEndReturnsFalseWhenFarFromEnd() {
        // 50 - 10 = 40, which is > PRE_FETCH_BEFORE_LAST (10)
        assertFalse(FeedPaginationUtils.isNearEnd(feedDetailsSize = 50, currentPage = 10))
    }

    @Test
    fun isNearEndReturnsTrueAtExactBoundary() {
        // feedDetailsSize - currentPage == PRE_FETCH_BEFORE_LAST
        val size = 30
        val page = size - PRE_FETCH_BEFORE_LAST
        assertTrue(FeedPaginationUtils.isNearEnd(feedDetailsSize = size, currentPage = page))
    }

    @Test
    fun isNearEndReturnsFalseJustOutsideBoundary() {
        // feedDetailsSize - currentPage == PRE_FETCH_BEFORE_LAST + 1
        val size = 30
        val page = size - PRE_FETCH_BEFORE_LAST - 1
        assertFalse(FeedPaginationUtils.isNearEnd(feedDetailsSize = size, currentPage = page))
    }

    @Test
    fun isNearEndReturnsTrueWhenCurrentPageExceedsFeedSize() {
        // Edge case: page beyond feed (size - page is negative, so <= threshold)
        assertTrue(FeedPaginationUtils.isNearEnd(feedDetailsSize = 5, currentPage = 10))
    }

    @Test
    fun isNearEndUpdatesAsCurrentPageAdvances() {
        val feedSize = 30
        // Start far from end
        assertFalse(FeedPaginationUtils.isNearEnd(feedSize, currentPage = 0))
        assertFalse(FeedPaginationUtils.isNearEnd(feedSize, currentPage = 10))
        assertFalse(FeedPaginationUtils.isNearEnd(feedSize, currentPage = 19))
        // Cross the threshold
        assertTrue(FeedPaginationUtils.isNearEnd(feedSize, currentPage = 20))
        assertTrue(FeedPaginationUtils.isNearEnd(feedSize, currentPage = 25))
        assertTrue(FeedPaginationUtils.isNearEnd(feedSize, currentPage = 29))
    }

    @Test
    fun isNearEndUpdatesAsFeedGrows() {
        val currentPage = 15
        // Near end with small feed
        assertTrue(FeedPaginationUtils.isNearEnd(feedDetailsSize = 20, currentPage = currentPage))
        // Not near end after more items load
        assertFalse(FeedPaginationUtils.isNearEnd(feedDetailsSize = 40, currentPage = currentPage))
    }

    // ── shouldTriggerPagination ────────────────────────────────────────

    @Test
    fun shouldTriggerPaginationWhenNearEndAndIdle() {
        assertTrue(
            FeedPaginationUtils.shouldTriggerPagination(
                feedDetailsSize = 20,
                currentPage = 18,
                isLoadingMore = false,
                pendingFetchDetails = 0,
            ),
        )
    }

    @Test
    fun shouldNotTriggerPaginationWhenAlreadyLoading() {
        assertFalse(
            FeedPaginationUtils.shouldTriggerPagination(
                feedDetailsSize = 20,
                currentPage = 18,
                isLoadingMore = true,
                pendingFetchDetails = 0,
            ),
        )
    }

    @Test
    fun shouldNotTriggerPaginationWhenDetailsFetchPending() {
        assertFalse(
            FeedPaginationUtils.shouldTriggerPagination(
                feedDetailsSize = 20,
                currentPage = 18,
                isLoadingMore = false,
                pendingFetchDetails = 3,
            ),
        )
    }

    @Test
    fun shouldNotTriggerPaginationWhenFarFromEnd() {
        assertFalse(
            FeedPaginationUtils.shouldTriggerPagination(
                feedDetailsSize = 50,
                currentPage = 5,
                isLoadingMore = false,
                pendingFetchDetails = 0,
            ),
        )
    }

    @Test
    fun shouldNotTriggerPaginationWhenFeedIsEmpty() {
        assertFalse(
            FeedPaginationUtils.shouldTriggerPagination(
                feedDetailsSize = 0,
                currentPage = 0,
                isLoadingMore = false,
                pendingFetchDetails = 0,
            ),
        )
    }

    // ── shouldShowLoader ───────────────────────────────────────────────

    @Test
    fun shouldShowLoaderWhenNearEndAndLoadingOnLastPage() {
        assertTrue(
            FeedPaginationUtils.shouldShowLoader(
                feedDetailsSize = 20,
                currentPage = 19,
                isLoadingMore = true,
                pendingFetchDetails = 0,
            ),
        )
    }

    @Test
    fun shouldShowLoaderWhenNearEndWithPendingDetailsOnLastPage() {
        assertTrue(
            FeedPaginationUtils.shouldShowLoader(
                feedDetailsSize = 20,
                currentPage = 19,
                isLoadingMore = false,
                pendingFetchDetails = 5,
            ),
        )
    }

    @Test
    fun shouldNotShowLoaderWhenNotOnLastPage() {
        assertFalse(
            FeedPaginationUtils.shouldShowLoader(
                feedDetailsSize = 20,
                currentPage = 15,
                isLoadingMore = true,
                pendingFetchDetails = 0,
            ),
        )
    }

    @Test
    fun shouldNotShowLoaderWhenNotLoadingAndNoPendingDetails() {
        assertFalse(
            FeedPaginationUtils.shouldShowLoader(
                feedDetailsSize = 20,
                currentPage = 19,
                isLoadingMore = false,
                pendingFetchDetails = 0,
            ),
        )
    }

    @Test
    fun shouldNotShowLoaderWhenFarFromEnd() {
        assertFalse(
            FeedPaginationUtils.shouldShowLoader(
                feedDetailsSize = 50,
                currentPage = 5,
                isLoadingMore = true,
                pendingFetchDetails = 0,
            ),
        )
    }
}
