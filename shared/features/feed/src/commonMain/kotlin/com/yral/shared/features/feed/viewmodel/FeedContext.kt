package com.yral.shared.features.feed.viewmodel

sealed class FeedContext {
    data object Default : FeedContext()

    data class Tournament(
        val tournamentId: String,
        val sessionKey: String? = null,
        val isHotOrNot: Boolean = false,
        val loadSavedPage: (suspend () -> Int?)? = null,
        val saveCurrentPage: (suspend (Int) -> Unit)? = null,
    ) : FeedContext()
}
