package com.yral.shared.features.subscriptions.ui

import com.yral.shared.core.session.ProDetails
import kotlinx.coroutines.flow.Flow

interface SubscriptionCoordinator {
    fun buySubscription()

    fun dismissSubscriptionBottomSheet()

    val proDetails: Flow<ProDetails?>
}
