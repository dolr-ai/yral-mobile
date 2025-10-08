package com.yral.shared.features.wallet.ui.btcRewards.nav

import com.arkivanov.decompose.ComponentContext

class DefaultVideoViewRewardsComponent(
    componentContext: ComponentContext,
    private val onDismissed: () -> Unit,
    private val navigateToWallet: () -> Unit,
    private val navigateToFeed: () -> Unit,
) : VideoViewRewardsComponent,
    ComponentContext by componentContext {
    override fun onDismissClicked() {
        onDismissed()
    }

    override fun openFeed() {
        navigateToFeed.invoke()
    }

    override fun openWallet() {
        navigateToWallet.invoke()
    }
}
