package com.yral.shared.features.account.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.MenuClickedEventData
import com.yral.shared.analytics.events.MenuCtaType
import com.yral.shared.analytics.events.MenuPageViewedEventData
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.analytics.AuthTelemetry

class AccountsTelemetry(
    private val analyticsManager: AnalyticsManager,
    private val authTelemetry: AuthTelemetry,
) {
    fun onMenuScreenViewed() {
        analyticsManager.trackEvent(MenuPageViewedEventData())
    }

    fun signUpClicked(pageName: SignupPageName) {
        authTelemetry.signupClicked(pageName)
    }

    fun onMenuClicked(ctaType: MenuCtaType) {
        analyticsManager.trackEvent(MenuClickedEventData(ctaType))
    }
}
