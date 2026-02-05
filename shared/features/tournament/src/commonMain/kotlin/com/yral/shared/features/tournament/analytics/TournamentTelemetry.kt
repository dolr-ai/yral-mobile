package com.yral.shared.features.tournament.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AnalyticsTournamentType
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.HowToPlayClickedEventData
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
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import com.yral.shared.features.tournament.domain.model.TournamentType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Suppress("TooManyFunctions")
@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class TournamentTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    // Session ID is generated once per app session (since TournamentTelemetry is a singleton)
    private val sessionId: String = Uuid.random().toString()

    private fun currentTimestamp(): String = Clock.System.now().toString()

    private fun getSessionId(): String = sessionId

    fun onTournamentScreenViewed(
        tournamentId: String,
        tournamentType: TournamentType,
    ) {
        analyticsManager.trackEvent(
            TournamentScreenViewedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                sessionId = getSessionId(),
            ),
        )
    }

    fun onRegistrationInitiated(
        tournamentId: String,
        tournamentType: TournamentType,
        entryFeePoints: Int,
        userPointBalance: Int,
        tournamentDurationMinutes: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentRegistrationInitiatedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                entryFeePoints = entryFeePoints,
                userPointBalance = userPointBalance,
                tournamentDuration = tournamentDurationMinutes,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onTournamentRegistered(
        tournamentId: String,
        tournamentType: TournamentType,
        entryFeePoints: Int?,
        entryFeeCredits: Int?,
    ) {
        analyticsManager.trackEvent(
            TournamentRegisteredEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                entryFeePoints = entryFeePoints,
                entryFeeCredits = entryFeeCredits,
                registrationTime = currentTimestamp(),
                sessionId = getSessionId(),
            ),
        )
    }

    fun onTournamentStateChangedIfChanged(
        previousTournament: Tournament,
        currentTournament: Tournament,
    ) {
        val previousState = previousTournament.toAnalyticsState()
        val currentState = currentTournament.toAnalyticsState()
        if (previousState == currentState) return

        val tokensRequired =
            if (currentState == TournamentState.ENDED) {
                null
            } else {
                currentTournament.participationState.tokensRequired()
            }
        val userDiamonds =
            if (currentState == TournamentState.ENDED) {
                null
            } else {
                currentTournament.participationState.userDiamonds()
            }
        analyticsManager.trackEvent(
            TournamentStateChangedEventData(
                tournamentId = currentTournament.id,
                tournamentType = currentTournament.type.toAnalyticsType(),
                fromState = previousState,
                toState = currentState,
                tokensRequired = tokensRequired,
                userDiamonds = userDiamonds,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onTournamentJoined(
        tournamentId: String,
        tournamentType: TournamentType,
        diamondsAllocated: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentJoinedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                joinTime = currentTimestamp(),
                diamondsAllocated = diamondsAllocated,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onAnswerSubmitted(
        tournamentId: String,
        tournamentType: TournamentType,
        isCorrect: Boolean,
        scoreDelta: Int,
        diamondsRemaining: Int,
        emojiShown: List<String>,
        userResponse: String,
        aiResponse: String,
    ) {
        analyticsManager.trackEvent(
            TournamentAnswerSubmittedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                answerResult = if (isCorrect) TournamentAnswerResult.RIGHT else TournamentAnswerResult.WRONG,
                scoreDelta = scoreDelta,
                diamondsRemaining = diamondsRemaining,
                sessionId = getSessionId(),
                emojiShown = emojiShown,
                userResponse = userResponse,
                aiResponse = aiResponse,
            ),
        )
    }

    fun onExitAttempted(
        tournamentId: String,
        tournamentType: TournamentType,
        diamondsRemaining: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentExitAttemptedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                diamondsRemaining = diamondsRemaining,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onExitNudgeShown(
        tournamentId: String,
        tournamentType: TournamentType,
    ) {
        analyticsManager.trackEvent(
            TournamentExitNudgeShownEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                sessionId = getSessionId(),
            ),
        )
    }

    fun onExitConfirmed(
        tournamentId: String,
        tournamentType: TournamentType,
        diamondsRemaining: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentExitConfirmedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                diamondsRemaining = diamondsRemaining,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onOutOfDiamondsShown(
        tournamentId: String,
        tournamentType: TournamentType,
    ) {
        analyticsManager.trackEvent(
            TournamentOutOfDiamondsShownEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                diamondsRemaining = 0,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onTournamentEnded(
        tournamentId: String,
        tournamentType: TournamentType,
        tournamentName: String,
    ) {
        analyticsManager.trackEvent(
            TournamentEndedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                tournamentName = tournamentName,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onResultScreenViewed(
        tournamentId: String,
        tournamentType: TournamentType,
        isWin: Boolean,
        finalScore: Int,
        rank: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentResultScreenViewedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                result = if (isWin) TournamentResult.WIN else TournamentResult.LOSE,
                finalScore = finalScore,
                rank = rank,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onLeaderboardViewed(
        tournamentId: String,
        tournamentType: TournamentType,
        userRank: Int,
        isWinner: Boolean,
    ) {
        analyticsManager.trackEvent(
            TournamentLeaderboardViewedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                userRank = userRank,
                isWinner = isWinner,
                sessionId = getSessionId(),
            ),
        )
    }

    fun onRewardEarned(
        tournamentId: String,
        tournamentType: TournamentType,
        rewardAmountInr: Int,
        rank: Int,
    ) {
        analyticsManager.trackEvent(
            TournamentRewardEarnedEventData(
                tournamentId = tournamentId,
                tournamentType = tournamentType.toAnalyticsType(),
                rewardAmountInr = rewardAmountInr,
                rank = rank,
            ),
        )
    }

    fun onHowToPlayClicked(tournamentType: TournamentType) {
        val gameType =
            when (tournamentType) {
                TournamentType.SMILEY -> GameType.SMILEY
                TournamentType.HOT_OR_NOT -> GameType.HOT_OR_NOT
            }
        analyticsManager.trackEvent(HowToPlayClickedEventData(gameType = gameType))
    }

    private fun TournamentParticipationState.toAnalyticsState(): TournamentState =
        when (this) {
            is TournamentParticipationState.RegistrationRequired -> TournamentState.REGISTRATION_REQUIRED
            is TournamentParticipationState.Registered -> TournamentState.REGISTERED
            is TournamentParticipationState.JoinNow -> TournamentState.JOIN_NOW
            is TournamentParticipationState.JoinNowWithTokens -> TournamentState.JOIN_NOW_WITH_TOKENS
            is TournamentParticipationState.JoinNowWithCredit -> TournamentState.JOIN_NOW_WITH_CREDIT
            is TournamentParticipationState.JoinNowDisabled -> TournamentState.JOIN_NOW_DISABLED
        }

    private fun Tournament.toAnalyticsState(): TournamentState =
        if (status is TournamentStatus.Ended) {
            TournamentState.ENDED
        } else {
            participationState.toAnalyticsState()
        }

    private fun TournamentParticipationState.tokensRequired(): Int? =
        when (this) {
            is TournamentParticipationState.RegistrationRequired -> tokensRequired
            is TournamentParticipationState.JoinNowWithTokens -> tokensRequired
            is TournamentParticipationState.JoinNowWithCredit -> null
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
            is TournamentParticipationState.JoinNowWithCredit,
            is TournamentParticipationState.JoinNowDisabled,
            -> null
        }

    private fun TournamentType.toAnalyticsType(): AnalyticsTournamentType =
        when (this) {
            TournamentType.SMILEY -> AnalyticsTournamentType.SMILEY
            TournamentType.HOT_OR_NOT -> AnalyticsTournamentType.HOT_OR_NOT
        }
}
