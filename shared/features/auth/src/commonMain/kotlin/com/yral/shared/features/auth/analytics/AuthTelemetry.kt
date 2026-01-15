package com.yral.shared.features.auth.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AnonymousAuthFailedEventData
import com.yral.shared.analytics.events.AuthFailedEventData
import com.yral.shared.analytics.events.AuthJourney
import com.yral.shared.analytics.events.AuthScreenViewedEventData
import com.yral.shared.analytics.events.AuthSessionCause
import com.yral.shared.analytics.events.AuthSessionFlow
import com.yral.shared.analytics.events.AuthSessionInitiator
import com.yral.shared.analytics.events.AuthSessionState
import com.yral.shared.analytics.events.AuthSessionStateChangedEventData
import com.yral.shared.analytics.events.LoginSuccessEventData
import com.yral.shared.analytics.events.SignupJourneySelected
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.analytics.events.SignupSuccessEventData
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.preferences.stores.AffiliateAttributionStore

class AuthTelemetry(
    private val analyticsManager: AnalyticsManager,
    private val affiliateAttributionStore: AffiliateAttributionStore,
) {
    fun onSignupViewed(pageName: SignupPageName) {
        analyticsManager.trackEvent(AuthScreenViewedEventData(pageName = pageName))
    }

    fun onSignupJourneySelected(provider: SocialProvider = SocialProvider.GOOGLE) {
        analyticsManager.trackEvent(SignupJourneySelected(authJourney = provider.toAuthJourney()))
    }

    fun onAuthSuccess(
        isNewUser: Boolean,
        provider: SocialProvider = SocialProvider.GOOGLE,
    ) {
        if (isNewUser) {
            onSignupSuccess(provider)
        } else {
            onLoginSuccess(provider)
        }
    }

    private fun onSignupSuccess(provider: SocialProvider) {
        val affiliate = affiliateAttributionStore.peek()
        analyticsManager.trackEvent(
            event =
                SignupSuccessEventData(
                    isReferral = false,
                    referralUserID = "",
                    authJourney = provider.toAuthJourney(),
                    affiliate = affiliate,
                ),
        )
    }

    private fun onLoginSuccess(provider: SocialProvider) {
        val affiliate = affiliateAttributionStore.peek()
        analyticsManager.trackEvent(
            LoginSuccessEventData(
                authJourney = provider.toAuthJourney(),
                affiliate = affiliate,
            ),
        )
    }

    fun authFailed(provider: SocialProvider = SocialProvider.GOOGLE) {
        val affiliate = affiliateAttributionStore.peek()
        analyticsManager.trackEvent(
            AuthFailedEventData(
                authJourney = provider.toAuthJourney(),
                affiliate = affiliate,
            ),
        )
    }

    fun anonymousAuthFailed(reason: String?) {
        val affiliate = affiliateAttributionStore.peek()
        analyticsManager.trackEvent(
            AnonymousAuthFailedEventData(
                affiliate = affiliate,
                reason = reason?.take(MAX_ERROR_MESSAGE_LENGTH),
            ),
        )
        analyticsManager.flush()
    }

    fun sessionStateChanged(
        fromState: AuthSessionState,
        toState: AuthSessionState,
        initiator: AuthSessionInitiator,
        cause: AuthSessionCause,
        flow: AuthSessionFlow? = null,
    ) {
        analyticsManager.trackEvent(
            AuthSessionStateChangedEventData(
                fromState = fromState,
                toState = toState,
                initiator = initiator,
                cause = cause,
                flow = flow,
            ),
        )
        analyticsManager.flush()
    }

    private fun SocialProvider.toAuthJourney(): AuthJourney =
        when (this) {
            SocialProvider.GOOGLE -> AuthJourney.GOOGLE
            SocialProvider.APPLE -> AuthJourney.APPLE
            SocialProvider.PHONE_NUMBER -> AuthJourney.PHONE
        }

    companion object {
        private const val MAX_ERROR_MESSAGE_LENGTH = 50
    }
}
