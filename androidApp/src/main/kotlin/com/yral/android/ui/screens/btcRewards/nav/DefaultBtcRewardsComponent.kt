package com.yral.android.ui.screens.btcRewards.nav

import com.arkivanov.decompose.ComponentContext

internal class DefaultBtcRewardsComponent(
    componentContext: ComponentContext,
    private val onDismissed: () -> Unit,
    private val navigateToWallet: () -> Unit,
    private val navigateToFeed: () -> Unit,
) : BtcRewardsComponent,
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
