package com.yral.shared.features.auth.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AuthFailedEventData
import com.yral.shared.analytics.events.AuthJourney
import com.yral.shared.analytics.events.AuthScreenViewedEventData
import com.yral.shared.analytics.events.LoginSuccessEventData
import com.yral.shared.analytics.events.SignupJourneySelected
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.analytics.events.SignupSuccessEventData
import com.yral.shared.preferences.AffiliateAttributionStore

class AuthTelemetry(
    private val analyticsManager: AnalyticsManager,
    private val affiliateAttributionStore: AffiliateAttributionStore,
) {
    fun onSignupViewed(pageName: SignupPageName) {
        analyticsManager.trackEvent(AuthScreenViewedEventData(pageName = pageName))
    }

    fun onSignupJourneySelected() {
        analyticsManager.trackEvent(SignupJourneySelected(authJourney = AuthJourney.GOOGLE))
    }

    fun onAuthSuccess(isNewUser: Boolean) {
        if (isNewUser) {
            onSignupSuccess()
        } else {
            onLoginSuccess()
        }
    }

    private fun onSignupSuccess() {
        val affiliate = affiliateAttributionStore.peek()
        analyticsManager.trackEvent(
            event =
                SignupSuccessEventData(
                    isReferral = false,
                    referralUserID = "",
                    authJourney = AuthJourney.GOOGLE,
                    affiliate = affiliate,
                ),
        )
    }

    private fun onLoginSuccess() {
        val affiliate = affiliateAttributionStore.peek()
        analyticsManager.trackEvent(
            LoginSuccessEventData(
                authJourney = AuthJourney.GOOGLE,
                affiliate = affiliate,
            ),
        )
    }

    fun authFailed() {
        val affiliate = affiliateAttributionStore.peek()
        analyticsManager.trackEvent(
            AuthFailedEventData(
                authJourney = AuthJourney.GOOGLE,
                affiliate = affiliate,
            ),
        )
    }
}
