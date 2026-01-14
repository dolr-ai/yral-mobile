package com.yral.shared.features.tournament.domain

import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.CastTournamentVoteRequest
import com.yral.shared.features.tournament.domain.model.GetMyTournamentsRequest
import com.yral.shared.features.tournament.domain.model.GetTournamentLeaderboardRequest
import com.yral.shared.features.tournament.domain.model.GetTournamentStatusRequest
import com.yral.shared.features.tournament.domain.model.GetTournamentsRequest
import com.yral.shared.features.tournament.domain.model.HotOrNotVoteRequest
import com.yral.shared.features.tournament.domain.model.HotOrNotVoteResult
import com.yral.shared.features.tournament.domain.model.RegisterForTournamentRequest
import com.yral.shared.features.tournament.domain.model.RegistrationResult
import com.yral.shared.features.tournament.domain.model.TournamentData
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentLeaderboard
import com.yral.shared.features.tournament.domain.model.TournamentStatusData
import com.yral.shared.features.tournament.domain.model.VoteResult

interface ITournamentRepository {
    /**
     * List tournaments with optional date/status filters.
     */
    suspend fun getTournaments(request: GetTournamentsRequest): Result<List<TournamentData>, TournamentError>

    /**
     * Get lightweight tournament status.
     */
    suspend fun getTournamentStatus(request: GetTournamentStatusRequest): Result<TournamentStatusData, TournamentError>

    /**
     * Register user for a tournament.
     */
    suspend fun registerForTournament(
        idToken: String,
        request: RegisterForTournamentRequest,
    ): Result<RegistrationResult, TournamentError>

    /**
     * List tournaments user has registered for.
     */
    suspend fun getMyTournaments(
        idToken: String,
        request: GetMyTournamentsRequest,
    ): Result<List<TournamentData>, TournamentError>

    /**
     * Cast vote during live tournament (smiley game).
     */
    suspend fun castVote(
        idToken: String,
        request: CastTournamentVoteRequest,
    ): Result<VoteResult, TournamentError>

    /**
     * Cast vote during live Hot or Not tournament.
     * Vote is compared against AI verdict.
     */
    suspend fun castHotOrNotVote(
        idToken: String,
        request: HotOrNotVoteRequest,
    ): Result<HotOrNotVoteResult, TournamentError>

    /**
     * Get tournament leaderboard.
     */
    suspend fun getTournamentLeaderboard(
        idToken: String,
        request: GetTournamentLeaderboardRequest,
    ): Result<TournamentLeaderboard, TournamentError>
}
