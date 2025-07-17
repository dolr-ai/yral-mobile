package com.yral.shared.features.auth.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AuthFailedEventData
import com.yral.shared.analytics.events.AuthJourney
import com.yral.shared.analytics.events.AuthScreenViewedEventData
import com.yral.shared.analytics.events.LoginSuccessEventData
import com.yral.shared.analytics.events.SignupClickedEventData
import com.yral.shared.analytics.events.SignupJourneySelected
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.analytics.events.SignupSuccessEventData

class AuthTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onSignupViewed(pageName: SignupPageName) {
        analyticsManager.trackEvent(AuthScreenViewedEventData(pageName = pageName))
    }

    fun onSignupJourneySelected() {
        analyticsManager.trackEvent(SignupJourneySelected(authJourney = AuthJourney.GOOGLE))
    }

    fun signupClicked(pageName: SignupPageName) {
        analyticsManager.trackEvent(SignupClickedEventData(pageName = pageName))
    }

    fun onAuthSuccess(isNewUser: Boolean) {
        if (isNewUser) {
            onSignupSuccess()
        } else {
            onLoginSuccess()
        }
    }

    private fun onSignupSuccess() {
        analyticsManager.trackEvent(
            event =
                SignupSuccessEventData(
                    isReferral = false,
                    referralUserID = "",
                    authJourney = AuthJourney.GOOGLE,
                ),
        )
    }

    private fun onLoginSuccess() {
        analyticsManager.trackEvent(LoginSuccessEventData(authJourney = AuthJourney.GOOGLE))
    }

    fun authFailed() {
        analyticsManager.trackEvent(AuthFailedEventData(authJourney = AuthJourney.GOOGLE))
    }
}
