package com.yral.android.ui.screens.feed.nav

import com.arkivanov.decompose.ComponentContext

interface FeedComponent {
    companion object Companion {
        operator fun invoke(componentContext: ComponentContext): FeedComponent = DefaultFeedComponent(componentContext)
    }
}
