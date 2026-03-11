package com.yral.shared.app.ui.screens.feed

import com.yral.shared.features.game.domain.models.VoteResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedScaffoldScreenTest {
    @Test
    fun shouldAutoScrollAfterVoteReturnsTrueForCurrentPageWithPendingResult() {
        assertTrue(
            shouldAutoScrollAfterVote(
                voteResult = VoteResult(coinDelta = 5, errorMessage = "", hasShownAnimation = false),
                showResultSheet = false,
                pageNo = 3,
                currentPage = 3,
            ),
        )
    }

    @Test
    fun shouldAutoScrollAfterVoteReturnsFalseWhenVoteAnimationAlreadyShown() {
        assertFalse(
            shouldAutoScrollAfterVote(
                voteResult = VoteResult(coinDelta = 5, errorMessage = "", hasShownAnimation = true),
                showResultSheet = false,
                pageNo = 3,
                currentPage = 3,
            ),
        )
    }

    @Test
    fun shouldAutoScrollAfterVoteReturnsFalseWhenResultSheetIsVisible() {
        assertFalse(
            shouldAutoScrollAfterVote(
                voteResult = VoteResult(coinDelta = 5, errorMessage = "", hasShownAnimation = false),
                showResultSheet = true,
                pageNo = 3,
                currentPage = 3,
            ),
        )
    }

    @Test
    fun shouldAutoScrollAfterVoteReturnsFalseForNonCurrentPage() {
        assertFalse(
            shouldAutoScrollAfterVote(
                voteResult = VoteResult(coinDelta = 5, errorMessage = "", hasShownAnimation = false),
                showResultSheet = false,
                pageNo = 2,
                currentPage = 3,
            ),
        )
    }
}
