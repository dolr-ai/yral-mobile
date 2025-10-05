package com.yral.shared.features.wallet.ui.btcRewards.nav

import com.arkivanov.decompose.ComponentContext

interface VideoViewRewardsComponent {
    fun onDismissClicked()
    fun openFeed()
    fun openWallet()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onDismissed: () -> Unit,
            navigateToWallet: () -> Unit,
            navigateToFeed: () -> Unit,
        ): VideoViewRewardsComponent =
            DefaultVideoViewRewardsComponent(
                componentContext,
                onDismissed,
                navigateToWallet,
                navigateToFeed,
            )
    }
}
