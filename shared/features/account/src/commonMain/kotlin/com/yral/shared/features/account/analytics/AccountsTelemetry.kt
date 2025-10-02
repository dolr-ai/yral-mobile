package com.yral.shared.features.account.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.MenuClickedEventData
import com.yral.shared.analytics.events.MenuCtaType
import com.yral.shared.analytics.events.MenuPageViewedEventData
import com.yral.shared.analytics.events.SignupClickedEventData
import com.yral.shared.analytics.events.SignupPageName

class AccountsTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onMenuScreenViewed() {
        analyticsManager.trackEvent(MenuPageViewedEventData())
    }

    fun signUpClicked(pageName: SignupPageName) {
        analyticsManager.trackEvent(SignupClickedEventData(pageName = pageName))
    }

    fun onMenuClicked(ctaType: MenuCtaType) {
        analyticsManager.trackEvent(MenuClickedEventData(ctaType))
    }
}
