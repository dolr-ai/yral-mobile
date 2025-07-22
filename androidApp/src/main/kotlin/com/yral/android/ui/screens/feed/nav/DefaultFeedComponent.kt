package com.yral.android.ui.screens.feed.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultFeedComponent(
    componentContext: ComponentContext,
    private val navigateToAccount: () -> Unit,
) : FeedComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun onAccountClicked() {
        navigateToAccount()
    }
}
