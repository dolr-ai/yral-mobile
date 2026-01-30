package com.yral.shared.features.subscriptions.nav

import com.yral.shared.core.session.ProDetails
import kotlinx.coroutines.flow.Flow

interface SubscriptionCoordinator {
    fun buySubscription()

    fun dismissSubscriptionBottomSheet()

    val proDetails: Flow<ProDetails>

    fun showSubscriptionNudge(content: SubscriptionNudgeContent)

    fun dismissSubscriptionNudge()

    fun refreshCreditBalances()

    var subscriptionNudgeContent: SubscriptionNudgeContent?
}
