@file:Suppress("MaxLineLength")

package com.yral.shared.features.tournament.data

import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.data.models.DailySessionRequestDto
import com.yral.shared.features.tournament.data.models.HotOrNotVoteRequestDto
import com.yral.shared.features.tournament.data.models.VideoEmojisRequestDto
import com.yral.shared.features.tournament.data.models.toDailySessionResult
import com.yral.shared.features.tournament.data.models.toRegistrationResult
import com.yral.shared.features.tournament.data.models.toTournamentDataList
import com.yral.shared.features.tournament.data.models.toTournamentLeaderboard
import com.yral.shared.features.tournament.data.models.toTournamentList
import com.yral.shared.features.tournament.data.models.toTournamentStatusData
import com.yral.shared.features.tournament.data.models.toVideoEmojisResult
import com.yral.shared.features.tournament.data.models.toVoteResult
import com.yral.shared.features.tournament.domain.ITournamentRepository
import com.yral.shared.features.tournament.domain.model.CastTournamentVoteRequest
import com.yral.shared.features.tournament.domain.model.DailySessionResult
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
import com.yral.shared.features.tournament.domain.model.VideoEmojisResult
import com.yral.shared.features.tournament.domain.model.VoteResult
import com.yral.shared.features.tournament.domain.model.toDto
import com.yral.shared.features.tournament.data.models.toVoteResult as toHotOrNotVoteResult

class TournamentRepository(
    private val remoteDataSource: ITournamentRemoteDataSource,
) : ITournamentRepository {
    override suspend fun getTournaments(request: GetTournamentsRequest): Result<List<TournamentData>, TournamentError> =
        remoteDataSource
            .getTournaments(request.toDto())
            .toTournamentList()

    override suspend fun getTournamentStatus(request: GetTournamentStatusRequest): Result<TournamentStatusData, TournamentError> =
        remoteDataSource
            .getTournamentStatus(request.toDto())
            .toTournamentStatusData()

    override suspend fun registerForTournament(
        idToken: String,
        request: RegisterForTournamentRequest,
    ): Result<RegistrationResult, TournamentError> =
        remoteDataSource
            .registerForTournament(idToken, request.toDto())
            .toRegistrationResult()

    override suspend fun getMyTournaments(
        idToken: String,
        request: GetMyTournamentsRequest,
    ): Result<List<TournamentData>, TournamentError> =
        remoteDataSource
            .getMyTournaments(idToken, request.toDto())
            .toTournamentDataList()

    override suspend fun castVote(
        idToken: String,
        request: CastTournamentVoteRequest,
    ): Result<VoteResult, TournamentError> =
        remoteDataSource
            .castVote(idToken, request.toDto())
            .toVoteResult()

    override suspend fun castHotOrNotVote(
        idToken: String,
        request: HotOrNotVoteRequest,
    ): Result<HotOrNotVoteResult, TournamentError> =
        remoteDataSource
            .castHotOrNotVote(
                idToken,
                HotOrNotVoteRequestDto(
                    tournamentId = request.tournamentId,
                    principalId = request.principalId,
                    videoId = request.videoId,
                    vote = request.vote,
                ),
            ).toHotOrNotVoteResult()

    override suspend fun getTournamentLeaderboard(
        idToken: String,
        request: GetTournamentLeaderboardRequest,
    ): Result<TournamentLeaderboard, TournamentError> =
        remoteDataSource
            .getTournamentLeaderboard(idToken, request.toDto())
            .toTournamentLeaderboard()

    override suspend fun getVideoEmojis(
        tournamentId: String,
        videoId: String,
    ): Result<VideoEmojisResult, Throwable> =
        remoteDataSource
            .getVideoEmojis(VideoEmojisRequestDto(tournamentId, videoId))
            .toVideoEmojisResult()

    override suspend fun startDailySession(
        idToken: String,
        tournamentId: String,
        principalId: String,
    ): Result<DailySessionResult, TournamentError> =
        remoteDataSource
            .startDailySession(idToken, DailySessionRequestDto(tournamentId, principalId))
            .toDailySessionResult()

    override suspend fun endDailySession(
        idToken: String,
        tournamentId: String,
        principalId: String,
    ): Result<DailySessionResult, TournamentError> =
        remoteDataSource
            .endDailySession(idToken, DailySessionRequestDto(tournamentId, principalId))
            .toDailySessionResult()
}
