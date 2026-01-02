package com.yral.shared.features.tournament.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.TournamentAnswerResult
import com.yral.shared.analytics.events.TournamentAnswerSubmittedEventData
import com.yral.shared.analytics.events.TournamentEndedEventData
import com.yral.shared.analytics.events.TournamentExitAttemptedEventData
import com.yral.shared.analytics.events.TournamentExitConfirmedEventData
import com.yral.shared.analytics.events.TournamentExitNudgeShownEventData
import com.yral.shared.analytics.events.TournamentJoinedEventData
import com.yral.shared.analytics.events.TournamentLeaderboardViewedEventData
import com.yral.shared.analytics.events.TournamentOutOfDiamondsShownEventData
import com.yral.shared.analytics.events.TournamentRegisteredEventData
import com.yral.shared.analytics.events.TournamentRegistrationInitiatedEventData
import com.yral.shared.analytics.events.TournamentResult
import com.yral.shared.analytics.events.TournamentResultScreenViewedEventData
import com.yral.shared.analytics.events.TournamentRewardEarnedEventData
import com.yral.shared.analytics.events.TournamentScreenViewedEventData
import com.yral.shared.analytics.events.TournamentState
import com.yral.shared.analytics.events.TournamentStateChangedEventData
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class TournamentTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    // Session ID is generated once per app session (since TournamentTelemetry is a singleton)
    private val sessionId: String = Uuid.random().toString()

    private fun currentTimestamp(): String = Clock.System.now().toString()

    private fun getSessionId(): String = sessionId

    fun onTournamentScreenViewed(tournamentId: String) {
        analyticsManager.trackEvent(
            TournamentScreenViewedEventData(
                tournamentId = tournamentId,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onRegistrationInitiated(
        tournamentId: String,
        entryFeePoints: Int,
        userPointBalance: Int,
        tournamentDurationMinutes: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentRegistrationInitiatedEventData(
                tournamentId = tournamentId,
                entryFeePoints = entryFeePoints,
                userPointBalance = userPointBalance,
                tournamentDuration = tournamentDurationMinutes,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onTournamentRegistered(
        tournamentId: String,
        entryFeePoints: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentRegisteredEventData(
                tournamentId = tournamentId,
                entryFeePoints = entryFeePoints,
                registrationTime = currentTimestamp(),
                sessionId = getSessionId(),
            ),
        )
    }

    fun onTournamentParticipationStateChanged(
        tournamentId: String,
        fromState: TournamentParticipationState,
        toState: TournamentParticipationState,
    ) {
        analyticsManager.trackEvent(
            TournamentStateChangedEventData(
                tournamentId = tournamentId,
                fromState = fromState.toAnalyticsState(),
                toState = toState.toAnalyticsState(),
                tokensRequired = toState.tokensRequired(),
                userDiamonds = toState.userDiamonds(),
                sessionId = getSessionId(),
            ),
        )
    }

    fun onTournamentJoined(
        tournamentId: String,
        diamondsAllocated: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentJoinedEventData(
                tournamentId = tournamentId,
                joinTime = currentTimestamp(),
                diamondsAllocated = diamondsAllocated,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onAnswerSubmitted(
        tournamentId: String,
        isCorrect: Boolean,
        scoreDelta: Int,
        diamondsRemaining: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentAnswerSubmittedEventData(
                tournamentId = tournamentId,
                answerResult = if (isCorrect) TournamentAnswerResult.CORRECT else TournamentAnswerResult.WRONG,
                scoreDelta = scoreDelta,
                diamondsRemaining = diamondsRemaining,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onExitAttempted(
        tournamentId: String,
        diamondsRemaining: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentExitAttemptedEventData(
                tournamentId = tournamentId,
                diamondsRemaining = diamondsRemaining,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onExitNudgeShown(tournamentId: String) {
        analyticsManager.trackEvent(
            TournamentExitNudgeShownEventData(
                tournamentId = tournamentId,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onExitConfirmed(
        tournamentId: String,
        diamondsRemaining: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentExitConfirmedEventData(
                tournamentId = tournamentId,
                diamondsRemaining = diamondsRemaining,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onOutOfDiamondsShown(tournamentId: String) {
        analyticsManager.trackEvent(
            TournamentOutOfDiamondsShownEventData(
                tournamentId = tournamentId,
                diamondsRemaining = 0,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onTournamentEnded(
        tournamentId: String,
        tournamentName: String,
    ) {
        analyticsManager.trackEvent(
            TournamentEndedEventData(
                tournamentId = tournamentId,
                tournamentName = tournamentName,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onResultScreenViewed(
        tournamentId: String,
        isWin: Boolean,
        finalScore: Int,
        rank: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentResultScreenViewedEventData(
                tournamentId = tournamentId,
                result = if (isWin) TournamentResult.WIN else TournamentResult.LOSE,
                finalScore = finalScore,
                rank = rank,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onLeaderboardViewed(
        tournamentId: String,
        userRank: Int,
        isWinner: Boolean,
    ) {
        analyticsManager.trackEvent(
            TournamentLeaderboardViewedEventData(
                tournamentId = tournamentId,
                userRank = userRank,
                isWinner = isWinner,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onRewardEarned(
        tournamentId: String,
        rewardAmountInr: Int,
        rank: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentRewardEarnedEventData(
                tournamentId = tournamentId,
                rewardAmountInr = rewardAmountInr,
                rank = rank,
            ),
        )
    }

    private fun TournamentParticipationState.toAnalyticsState(): TournamentState =
        when (this) {
            is TournamentParticipationState.RegistrationRequired -> TournamentState.REGISTRATION_REQUIRED
            is TournamentParticipationState.Registered -> TournamentState.REGISTERED
            is TournamentParticipationState.JoinNow -> TournamentState.JOIN_NOW
            is TournamentParticipationState.JoinNowWithTokens -> TournamentState.JOIN_NOW_WITH_TOKENS
            is TournamentParticipationState.JoinNowDisabled -> TournamentState.JOIN_NOW_DISABLED
        }

    private fun TournamentParticipationState.tokensRequired(): Int? =
        when (this) {
            is TournamentParticipationState.RegistrationRequired -> tokensRequired
            is TournamentParticipationState.JoinNowWithTokens -> tokensRequired
            is TournamentParticipationState.JoinNow,
            is TournamentParticipationState.JoinNowDisabled,
            is TournamentParticipationState.Registered,
            -> null
        }

    private fun TournamentParticipationState.userDiamonds(): Int? =
        when (this) {
            is TournamentParticipationState.JoinNow -> userDiamonds
            is TournamentParticipationState.RegistrationRequired,
            is TournamentParticipationState.Registered,
            is TournamentParticipationState.JoinNowWithTokens,
            is TournamentParticipationState.JoinNowDisabled,
            -> null
        }
}
