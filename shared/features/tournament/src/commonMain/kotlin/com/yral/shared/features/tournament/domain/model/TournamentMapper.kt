package com.yral.shared.features.tournament.domain.model

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Maps API TournamentData to UI Tournament model.
 */
@OptIn(ExperimentalTime::class)
fun TournamentData.toUiTournament(): Tournament {
    val startTime = Instant.fromEpochMilliseconds(startEpochMs)
    val endTime = Instant.fromEpochMilliseconds(endEpochMs)
    val currentTime = Clock.System.now()
    val tournamentStatus =
        when {
            currentTime < startTime -> TournamentStatus.Upcoming(startTime = startTime)
            currentTime > endTime -> TournamentStatus.Ended
            else -> TournamentStatus.Live(endTime = endTime)
        }

    val participationState =
        when {
            userStats != null -> {
                when (tournamentStatus) {
                    is TournamentStatus.Live -> TournamentParticipationState.JoinNow
                    is TournamentStatus.Upcoming -> {
                        val timeLeft = startTime - currentTime
                        if (timeLeft <= 10.minutes) {
                            TournamentParticipationState.JoinNowDisabled
                        } else {
                            TournamentParticipationState.Registered
                        }
                    }
                    else -> TournamentParticipationState.Registered
                }
            }
            tournamentStatus is TournamentStatus.Live -> {
                TournamentParticipationState.JoinNowWithTokens(entryCost)
            }
            else -> {
                TournamentParticipationState.RegistrationRequired(entryCost)
            }
        }

    val scheduleLabel = formatScheduleLabel(date, startTime.toHourMinute12h(), endTime.toHourMinute12h())
    val participantsLabel = formatParticipantsLabel(participantCount, tournamentStatus)

    return Tournament(
        id = id,
        title = title,
        totalPrizePool = totalPrizePool,
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

@Suppress("MagicNumber")
@OptIn(ExperimentalTime::class)
private fun Instant.toHourMinute12h(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = this.toLocalDateTime(timeZone)
    val hour12 = ((dt.hour + 11) % 12 + 1)
    val amPm = if (dt.hour < 12) "am" else "pm"

    return "${hour12.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')} $amPm"
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
    status: TournamentStatus,
): String =
    when (status) {
        TournamentStatus.Ended -> "$count Participants"
        is TournamentStatus.Live -> "$count Playing"
        is TournamentStatus.Upcoming -> "$count Registered"
    }

private fun formatRankLabel(rank: Int): String = "$rank${getOrdinalSuffix(rank)} Place"
