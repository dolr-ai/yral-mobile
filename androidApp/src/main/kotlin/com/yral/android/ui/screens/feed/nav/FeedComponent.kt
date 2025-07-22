package com.yral.android.ui.screens.feed.nav

import com.arkivanov.decompose.ComponentContext

interface FeedComponent {
    fun onAccountClicked()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            navigateToAccount: () -> Unit,
        ): FeedComponent =
            DefaultFeedComponent(
                componentContext = componentContext,
                navigateToAccount = navigateToAccount,
            )
    }
}
