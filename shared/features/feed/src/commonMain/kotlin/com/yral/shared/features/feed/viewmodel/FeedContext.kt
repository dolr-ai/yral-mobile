package com.yral.shared.features.feed.viewmodel

sealed class FeedContext {
    data object Default : FeedContext()

    data class Tournament(
        val tournamentId: String,
        val sessionKey: String? = null,
    ) : FeedContext()
}
