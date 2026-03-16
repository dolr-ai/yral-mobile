package com.yral.shared.features.feed.ui

import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.PRE_FETCH_BEFORE_LAST

/**
 * Pure pagination logic extracted from FeedScreen for testability.
 *
 * IMPORTANT: In FeedScreen.kt, these reads MUST happen inside `derivedStateOf`
 * (not captured as plain locals outside it), so Compose can track state changes.
 */
internal object FeedPaginationUtils {
    /**
     * Whether the user has scrolled close enough to the end of the feed
     * that we should pre-fetch more content.
     */
    fun isNearEnd(
        feedDetailsSize: Int,
        currentPage: Int,
    ): Boolean = feedDetailsSize > 0 && (feedDetailsSize - currentPage) <= PRE_FETCH_BEFORE_LAST

    /**
     * Whether pagination should actually trigger (near end + not already loading).
     */
    fun shouldTriggerPagination(
        feedDetailsSize: Int,
        currentPage: Int,
        isLoadingMore: Boolean,
        pendingFetchDetails: Int,
    ): Boolean =
        isNearEnd(feedDetailsSize, currentPage) &&
            !isLoadingMore &&
            pendingFetchDetails == 0

    /**
     * Whether the loading indicator should be visible at the end of the feed.
     */
    fun shouldShowLoader(
        feedDetailsSize: Int,
        currentPage: Int,
        isLoadingMore: Boolean,
        pendingFetchDetails: Int,
    ): Boolean =
        isNearEnd(feedDetailsSize, currentPage) &&
            (isLoadingMore || pendingFetchDetails > 0) &&
            currentPage == feedDetailsSize - 1
}
