package com.yral.shared.features.tournament.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.tournament.domain.model.TournamentData
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TournamentListResponseDto {
    @Serializable
    data class Success(
        @SerialName("tournaments")
        val tournaments: List<TournamentDto>,
    ) : TournamentListResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: TournamentErrorDto,
    ) : TournamentListResponseDto()
}

@Serializable
data class TournamentDto(
    @SerialName("id")
    val id: String,
    @SerialName("date")
    val date: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("start_epoch_ms")
    val startEpochMs: Long,
    @SerialName("end_epoch_ms")
    val endEpochMs: Long,
    @SerialName("entry_cost")
    val entryCost: Int,
    @SerialName("total_prize_pool")
    val totalPrizePool: Int,
    @SerialName("status")
    val status: String,
    @SerialName("prize_map")
    val prizeMap: Map<String, Int>,
    @SerialName("participant_count")
    val participantCount: Int,
)

fun TournamentListResponseDto.toTournamentList(): Result<List<TournamentData>, TournamentError> =
    when (this) {
        is TournamentListResponseDto.Success -> {
            Ok(tournaments.map { it.toTournamentData() })
        }
        is TournamentListResponseDto.Error -> {
            Err(error.toTournamentError())
        }
    }

fun TournamentDto.toTournamentData(): TournamentData =
    TournamentData(
        id = id,
        date = date,
        startTime = startTime,
        endTime = endTime,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        entryCost = entryCost,
        totalPrizePool = totalPrizePool,
        status = status,
        prizeMap =
            prizeMap
                .mapNotNull { (key, value) ->
                    key.toIntOrNull()?.let { it to value }
                }.toMap(),
        participantCount = participantCount,
    )

fun TournamentErrorDto.toTournamentError(): TournamentError =
    TournamentError(
        code = TournamentErrorCodes.fromCode(code),
        message = message,
    )
