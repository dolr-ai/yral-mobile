package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.TournamentData
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentType
import com.yral.shared.features.tournament.domain.model.UserTournamentStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MyTournamentsResponseDto {
    @Serializable
    data class Success(
        @SerialName("tournaments")
        val tournaments: List<MyTournamentDto>,
    ) : MyTournamentsResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: TournamentErrorDto,
    ) : MyTournamentsResponseDto()
}

@Serializable
data class MyTournamentDto(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("type")
    val type: String? = null,
    @SerialName("date")
    val date: String,
    @SerialName("start_epoch_ms")
    val startEpochMs: Long,
    @SerialName("end_epoch_ms")
    val endEpochMs: Long,
    @SerialName("entry_cost")
    val entryCost: Int,
    @SerialName("total_prize_pool")
    val totalPrizePool: Int,
    @SerialName("prize_map")
    val prizeMap: Map<String, Int>,
    @SerialName("participant_count")
    val participantCount: Int = 0,
    @SerialName("user_stats")
    val userStats: UserStatsDto,
)

@Serializable
data class UserStatsDto(
    @SerialName("coins_paid")
    val coinsPaid: Int? = null,
    @SerialName("diamonds")
    val diamonds: Int = 0,
    @SerialName("tournament_wins")
    val tournamentWins: Int = 0,
    @SerialName("tournament_losses")
    val tournamentLosses: Int = 0,
    @SerialName("status")
    val status: String? = null,
)

fun MyTournamentsResponseDto.toTournamentDataList(): Result<List<TournamentData>, TournamentError> =
    when (this) {
        is MyTournamentsResponseDto.Success -> {
            Ok(tournaments.map { it.toTournamentData() })
        }
        is MyTournamentsResponseDto.Error -> {
            Err(error.toTournamentError())
        }
    }

fun MyTournamentDto.toTournamentData(): TournamentData =
    TournamentData(
        id = id,
        title = title,
        type = TournamentType.fromString(type),
        date = date,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        entryCost = entryCost,
        totalPrizePool = totalPrizePool,
        prizeMap =
            prizeMap
                .mapNotNull { (key, value) ->
                    key.toIntOrNull()?.let { it to value }
                }.toMap(),
        participantCount = participantCount,
        userStats =
            UserTournamentStats(
                coinsPaid = userStats.coinsPaid ?: 0,
                tournamentWins = userStats.tournamentWins,
                tournamentLosses = userStats.tournamentLosses,
                registrationStatus = userStats.status ?: "",
                diamonds = userStats.diamonds,
            ),
    )
