package com.yral.shared.features.tournament.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.TournamentAnswerResult
import com.yral.shared.analytics.events.TournamentAnswerSubmittedEventData
import com.yral.shared.analytics.events.TournamentCtaState
import com.yral.shared.analytics.events.TournamentEndedEventData
import com.yral.shared.analytics.events.TournamentExitAttemptedEventData
import com.yral.shared.analytics.events.TournamentExitConfirmedEventData
import com.yral.shared.analytics.events.TournamentExitNudgeShownEventData
import com.yral.shared.analytics.events.TournamentJoinCtaActivatedEventData
import com.yral.shared.analytics.events.TournamentJoinCtaViewedEventData
import com.yral.shared.analytics.events.TournamentJoinedEventData
import com.yral.shared.analytics.events.TournamentLeaderboardViewedEventData
import com.yral.shared.analytics.events.TournamentOutOfDiamondsShownEventData
import com.yral.shared.analytics.events.TournamentRegisteredEventData
import com.yral.shared.analytics.events.TournamentRegistrationInitiatedEventData
import com.yral.shared.analytics.events.TournamentResult
import com.yral.shared.analytics.events.TournamentResultScreenViewedEventData
import com.yral.shared.analytics.events.TournamentRewardEarnedEventData
import com.yral.shared.analytics.events.TournamentScreenViewedEventData
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

    fun onJoinCtaViewed(
        tournamentId: String,
        isActive: Boolean,
        timeToStartSec: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentJoinCtaViewedEventData(
                tournamentId = tournamentId,
                ctaState = if (isActive) TournamentCtaState.ACTIVE else TournamentCtaState.INACTIVE,
                timeToStartSec = timeToStartSec,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onJoinCtaActivated(tournamentId: String) {
        analyticsManager.trackEvent(
            TournamentJoinCtaActivatedEventData(
                tournamentId = tournamentId,
                ctaState = TournamentCtaState.ACTIVE,
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
}
