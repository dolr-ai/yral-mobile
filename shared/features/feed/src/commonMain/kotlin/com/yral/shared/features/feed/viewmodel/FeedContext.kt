package com.yral.shared.features.feed.viewmodel

sealed class FeedContext {
    data object Default : FeedContext()
}
