package com.yral.shared.features.tournament.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Maps API TournamentData to UI Tournament model.
 */
@OptIn(ExperimentalTime::class)
fun TournamentData.toUiTournament(isRegistered: Boolean = false): Tournament {
    val tournamentStatus =
        when (status.lowercase()) {
            "live" ->
                TournamentStatus.Live(
                    endTime = Instant.fromEpochMilliseconds(endEpochMs),
                )
            "ended", "settled", "cancelled" -> TournamentStatus.Ended
            else ->
                TournamentStatus.Upcoming(
                    startTime = Instant.fromEpochMilliseconds(startEpochMs),
                )
        }

    val participationState =
        when {
            isRegistered || userStats != null -> {
                when (status.lowercase()) {
                    "live" -> TournamentParticipationState.JoinNow
                    else -> TournamentParticipationState.Registered
                }
            }
            status.lowercase() == "live" -> {
                TournamentParticipationState.JoinNowWithTokens(entryCost)
            }
            else -> {
                TournamentParticipationState.RegistrationRequired(entryCost)
            }
        }

    val scheduleLabel = formatScheduleLabel(date, startTime, endTime)
    val participantsLabel = formatParticipantsLabel(participantCount, status)

    return Tournament(
        id = id,
        title = title,
        subtitle = "Win up to ₹$totalPrizePool",
        participantsLabel = participantsLabel,
        scheduleLabel = scheduleLabel,
        status = tournamentStatus,
        participationState = participationState,
        prizeBreakdown =
            prizeMap.entries
                .sortedBy { it.key }
                .map { (rank, amount) ->
                    PrizeBreakdownRow(
                        rankLabel = formatRankLabel(rank),
                        amountLabel = "₹$amount",
                    )
                },
    )
}

private val MONTH_NAMES =
    listOf(
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec",
    )

private const val DATE_PARTS_COUNT = 3
private const val ORDINAL_TEEN_START = 11
private const val ORDINAL_TEEN_END = 13
private const val ORDINAL_DIVISOR = 10
private const val ORDINAL_FIRST = 1
private const val ORDINAL_SECOND = 2
private const val ORDINAL_THIRD = 3

@Suppress("ReturnCount")
private fun formatScheduleLabel(
    date: String,
    startTime: String,
    endTime: String,
): String {
    val fallback = "$date • $startTime-$endTime"
    val parts = date.split("-")
    if (parts.size != DATE_PARTS_COUNT) return fallback

    val month = parts[1].toIntOrNull() ?: return fallback
    val day = parts[2].toIntOrNull() ?: return fallback

    val monthName = MONTH_NAMES.getOrNull(month - 1) ?: return fallback
    val daySuffix = getOrdinalSuffix(day)

    return "$monthName $day$daySuffix • $startTime-$endTime"
}

private fun getOrdinalSuffix(number: Int): String =
    when {
        number in ORDINAL_TEEN_START..ORDINAL_TEEN_END -> "th"
        number % ORDINAL_DIVISOR == ORDINAL_FIRST -> "st"
        number % ORDINAL_DIVISOR == ORDINAL_SECOND -> "nd"
        number % ORDINAL_DIVISOR == ORDINAL_THIRD -> "rd"
        else -> "th"
    }

private fun formatParticipantsLabel(
    count: Int,
    status: String,
): String =
    when (status.lowercase()) {
        "live" -> "$count Playing"
        "ended", "settled" -> "$count Participants"
        else -> "$count Registered"
    }

private fun formatRankLabel(rank: Int): String = "$rank${getOrdinalSuffix(rank)} Place"
