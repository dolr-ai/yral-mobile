package com.yral.android.ui.screens.btcRewards.nav

import com.arkivanov.decompose.ComponentContext

interface BtcRewardsComponent {
    fun onDismissClicked()
    fun openFeed()
    fun openWallet()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onDismissed: () -> Unit,
            navigateToWallet: () -> Unit,
            navigateToFeed: () -> Unit,
        ): BtcRewardsComponent =
            DefaultBtcRewardsComponent(
                componentContext,
                onDismissed,
                navigateToWallet,
                navigateToFeed,
            )
    }
}
