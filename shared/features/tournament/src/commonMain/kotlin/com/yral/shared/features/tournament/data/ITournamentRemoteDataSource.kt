package com.yral.shared.features.tournament.data

import com.yral.shared.features.tournament.data.models.HotOrNotVoteRequestDto
import com.yral.shared.features.tournament.data.models.HotOrNotVoteResponseDto
import com.yral.shared.features.tournament.data.models.MyTournamentsRequestDto
import com.yral.shared.features.tournament.data.models.MyTournamentsResponseDto
import com.yral.shared.features.tournament.data.models.RegisterTournamentRequestDto
import com.yral.shared.features.tournament.data.models.RegisterTournamentResponseDto
import com.yral.shared.features.tournament.data.models.TournamentLeaderboardRequestDto
import com.yral.shared.features.tournament.data.models.TournamentLeaderboardResponseDto
import com.yral.shared.features.tournament.data.models.TournamentListRequestDto
import com.yral.shared.features.tournament.data.models.TournamentListResponseDto
import com.yral.shared.features.tournament.data.models.TournamentStatusRequestDto
import com.yral.shared.features.tournament.data.models.TournamentStatusResponseDto
import com.yral.shared.features.tournament.data.models.TournamentVoteRequestDto
import com.yral.shared.features.tournament.data.models.TournamentVoteResponseDto
import com.yral.shared.features.tournament.data.models.VideoEmojisRequestDto
import com.yral.shared.features.tournament.data.models.VideoEmojisResponseDto

interface ITournamentRemoteDataSource {
    /**
     * List tournaments with optional date/status filters.
     * No authentication required.
     */
    suspend fun getTournaments(request: TournamentListRequestDto): TournamentListResponseDto

    /**
     * Get lightweight tournament status.
     * No authentication required.
     */
    suspend fun getTournamentStatus(request: TournamentStatusRequestDto): TournamentStatusResponseDto

    /**
     * Register user for a tournament.
     * Requires authentication.
     */
    suspend fun registerForTournament(
        idToken: String,
        request: RegisterTournamentRequestDto,
    ): RegisterTournamentResponseDto

    /**
     * List tournaments user has registered for.
     * Requires authentication.
     */
    suspend fun getMyTournaments(
        idToken: String,
        request: MyTournamentsRequestDto,
    ): MyTournamentsResponseDto

    /**
     * Cast vote during live tournament (smiley game).
     * Requires authentication.
     */
    suspend fun castVote(
        idToken: String,
        request: TournamentVoteRequestDto,
    ): TournamentVoteResponseDto

    /**
     * Cast vote during live Hot or Not tournament.
     * Vote is compared against AI verdict.
     * Requires authentication.
     */
    suspend fun castHotOrNotVote(
        idToken: String,
        request: HotOrNotVoteRequestDto,
    ): HotOrNotVoteResponseDto

    /**
     * Get tournament leaderboard.
     * Requires authentication.
     */
    suspend fun getTournamentLeaderboard(
        idToken: String,
        request: TournamentLeaderboardRequestDto,
    ): TournamentLeaderboardResponseDto

    /**
     * Get video-specific emojis for a tournament video.
     * Used for prefetching emoji data before user sees the video.
     * No authentication required.
     */
    suspend fun getVideoEmojis(request: VideoEmojisRequestDto): VideoEmojisResponseDto
}
