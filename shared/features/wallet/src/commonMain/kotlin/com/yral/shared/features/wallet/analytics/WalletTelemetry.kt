package com.yral.shared.features.wallet.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.HowToEarnClickedEventData
import com.yral.shared.analytics.events.VideoViewsRewardsNudgeShownEventData
import com.yral.shared.analytics.events.WalletPageViewedEventData
import com.yral.shared.libs.routing.routes.api.RewardsReceived

class WalletTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onWalletScreenViewed() {
        analyticsManager.trackEvent(WalletPageViewedEventData())
    }

    fun onHowToEarnClicked() {
        analyticsManager.trackEvent(HowToEarnClickedEventData())
    }

    fun onVideoViewsRewardsNudgeShown(data: RewardsReceived) {
        analyticsManager.trackEvent(
            event =
                VideoViewsRewardsNudgeShownEventData(
                    videoId = data.videoID,
                    currentViews = data.viewCount?.toLong(),
                    rewardAmountBtc = data.rewardBtc?.toDouble(),
                ),
        )
    }
}
