package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.core.session.ProDetails
import com.yral.shared.features.tournament.domain.model.PrizeBreakdownRow
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import com.yral.shared.features.tournament.domain.model.TournamentType
import com.yral.shared.libs.designsystem.modifierx.conditional
import com.yral.shared.libs.designsystem.modifierx.grayScale
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.bitcoin
import yral_mobile.shared.features.tournament.generated.resources.ended
import yral_mobile.shared.features.tournament.generated.resources.game_live_time
import yral_mobile.shared.features.tournament.generated.resources.game_starts_at_today
import yral_mobile.shared.features.tournament.generated.resources.game_tomorrow_time
import yral_mobile.shared.features.tournament.generated.resources.hot_or_not_subtitle
import yral_mobile.shared.features.tournament.generated.resources.hot_or_not_tournament_banner
import yral_mobile.shared.features.tournament.generated.resources.ic_calendar
import yral_mobile.shared.features.tournament.generated.resources.ic_question_circle
import yral_mobile.shared.features.tournament.generated.resources.ic_ranking
import yral_mobile.shared.features.tournament.generated.resources.ic_share
import yral_mobile.shared.features.tournament.generated.resources.ic_timer
import yral_mobile.shared.features.tournament.generated.resources.ic_trophy_cup
import yral_mobile.shared.features.tournament.generated.resources.ic_users
import yral_mobile.shared.features.tournament.generated.resources.play_game_in
import yral_mobile.shared.features.tournament.generated.resources.play_title
import yral_mobile.shared.features.tournament.generated.resources.prize_label
import yral_mobile.shared.features.tournament.generated.resources.smiley_subtitle
import yral_mobile.shared.features.tournament.generated.resources.smiley_tournament_banner
import yral_mobile.shared.features.tournament.generated.resources.starts_in
import yral_mobile.shared.features.tournament.generated.resources.win_upto_prize
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import yral_mobile.shared.features.tournament.generated.resources.Res as TournamentRes

private const val TEN_MINUTES_MS = 10 * 60 * 1000f

@Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalTime::class)
@Composable
fun TournamentCard(
    tournament: Tournament,
    proDetails: ProDetails,
    onPrizeBreakdownClick: () -> Unit,
    onShareClick: () -> Unit,
    onTournamentCtaClick: () -> Unit,
) {
    val now by produceState(initialValue = Clock.System.now(), tournament.status) {
        if (tournament.status is TournamentStatus.Ended) {
            return@produceState
        }
        while (true) {
            value = Clock.System.now()
            delay(1.seconds)
        }
    }

    val currentParticipationState = deriveParticipationState(tournament, now, proDetails)

    val colors = tournamentCardColors(tournament.type)
    val cardShape = RoundedCornerShape(8.dp)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .wrapContentHeight(),
    ) {
        TournamentCardHeader(
            tournament = tournament,
            now = now,
            headerBgColor = colors.headerBg,
            onShareClick = onShareClick,
        )

        TournamentCardBody(
            tournament = tournament,
            now = now,
            currentParticipationState = currentParticipationState,
            colors = colors,
            onPrizeBreakdownClick = onPrizeBreakdownClick,
            onTournamentCtaClick = onTournamentCtaClick,
        )
    }
}

@Suppress("MagicNumber")
@OptIn(ExperimentalTime::class)
@Composable
private fun TournamentCardHeader(
    tournament: Tournament,
    now: Instant,
    headerBgColor: Color,
    onShareClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(headerBgColor),
    ) {
        Image(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .conditional(
                        condition = tournament.status is TournamentStatus.Ended,
                        ifTrue = { grayScale() },
                    ),
            painter =
                painterResource(
                    when (tournament.type) {
                        TournamentType.SMILEY -> TournamentRes.drawable.smiley_tournament_banner
                        TournamentType.HOT_OR_NOT -> TournamentRes.drawable.hot_or_not_tournament_banner
                    },
                ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )

        TimerPillBadge(
            status = tournament.status,
            now = now,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp),
        )

        if (tournament.status !is TournamentStatus.Ended) {
            Icon(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .clickable(onClick = onShareClick)
                        .padding(top = 12.dp, end = 12.dp)
                        .size(20.dp),
                painter = painterResource(TournamentRes.drawable.ic_share),
                contentDescription = "Share",
                tint = Color.Unspecified,
            )
        }
    }
}

@Suppress("MagicNumber")
private fun pillStyle(status: TournamentStatus): Pair<Brush, Color> =
    when (status) {
        is TournamentStatus.Live ->
            Brush.radialGradient(
                colors = listOf(Color(0xFFC62C2C), Color(0xFF9C1F1F), Color(0xFF731212)),
            ) to Color(0xFF4A0606)
        is TournamentStatus.Upcoming ->
            Brush.radialGradient(
                colors = listOf(Color(0xFF2C8C2C), Color(0xFF1F6F1F), Color(0xFF124A12)),
            ) to Color(0xFF0A2E0A)
        TournamentStatus.Ended ->
            Brush.linearGradient(
                colors = listOf(Color(0xFF4A4A4A), Color(0xFF3A3A3A)),
            ) to Color(0xFF2A2A2A)
    }

@Composable
@OptIn(ExperimentalTime::class)
private fun TimerPillBadge(
    status: TournamentStatus,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(999.dp)
    val (pillBackground, pillBorderColor) = pillStyle(status)
    val label =
        when (status) {
            is TournamentStatus.Live ->
                stringResource(TournamentRes.string.game_live_time, formatRemainingDuration(status.endTime - now))
            is TournamentStatus.Upcoming ->
                stringResource(TournamentRes.string.starts_in, formatRemainingDuration(status.startTime - now))
            TournamentStatus.Ended -> stringResource(TournamentRes.string.ended)
        }

    Row(
        modifier =
            modifier
                .clip(pillShape)
                .background(pillBackground)
                .border(width = 1.dp, color = pillBorderColor, shape = pillShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(12.dp),
            painter = painterResource(TournamentRes.drawable.ic_timer),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Text(
            text = label,
            style = LocalAppTopography.current.regSemiBold,
            color = YralColors.NeutralIconsActive,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Suppress("MagicNumber")
@Composable
@OptIn(ExperimentalTime::class)
private fun scheduleText(
    status: TournamentStatus,
    startEpochMs: Long,
    endEpochMs: Long,
    now: Instant,
): String =
    when (status) {
        is TournamentStatus.Live -> {
            val tz = TimeZone.currentSystemDefault()
            val startDt = Instant.fromEpochMilliseconds(startEpochMs).toLocalDateTime(tz)
            val endDt = Instant.fromEpochMilliseconds(endEpochMs).toLocalDateTime(tz)
            val startStr = formatHourMinute12h(startDt.hour, startDt.minute)
            val endStr = formatHourMinute12h(endDt.hour, endDt.minute)
            val timeRange = "$startStr-$endStr"
            stringResource(TournamentRes.string.game_live_time, timeRange)
        }
        is TournamentStatus.Upcoming -> {
            val tz = TimeZone.currentSystemDefault()
            val nowDate = now.toLocalDateTime(tz).date
            val startInstant = Instant.fromEpochMilliseconds(startEpochMs)
            val startDt = startInstant.toLocalDateTime(tz)
            val dayDiff = startDt.date.toEpochDays() - nowDate.toEpochDays()
            val timeStr = formatHourMinute12h(startDt.hour, startDt.minute)
            when (dayDiff) {
                0L -> stringResource(TournamentRes.string.game_starts_at_today, timeStr)
                1L -> stringResource(TournamentRes.string.game_tomorrow_time, timeStr)
                else -> stringResource(TournamentRes.string.starts_in, formatRemainingDuration(startInstant - now))
            }
        }
        TournamentStatus.Ended -> stringResource(TournamentRes.string.ended)
    }

@Suppress("MagicNumber", "LongMethod")
@OptIn(ExperimentalTime::class)
@Composable
private fun TournamentCardBody(
    tournament: Tournament,
    now: Instant,
    currentParticipationState: TournamentParticipationState,
    colors: TournamentCardColors,
    onPrizeBreakdownClick: () -> Unit,
    onTournamentCtaClick: () -> Unit,
) {
    val fillFraction: Float
    val countdownText: String?

    if (currentParticipationState == TournamentParticipationState.JoinNowDisabled) {
        val startTime = Instant.fromEpochMilliseconds(tournament.startEpochMs)
        val timeRemainingMs = (startTime - now).inWholeMilliseconds.coerceAtLeast(0)
        fillFraction = (1f - (timeRemainingMs.toFloat() / TEN_MINUTES_MS)).coerceIn(0f, 1f)
        countdownText = stringResource(TournamentRes.string.play_game_in, formatRemainingDuration(startTime - now))
    } else {
        fillFraction = 0f
        countdownText = null
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.bodyBg)
                .padding(10.dp),
    ) {
        // Title row with info icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(TournamentRes.string.play_title, tournament.title),
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                modifier =
                    Modifier
                        .clickable(onClick = onPrizeBreakdownClick)
                        .padding(4.dp)
                        .size(20.dp),
                painter = painterResource(TournamentRes.drawable.ic_question_circle),
                contentDescription = "How to play",
                tint = Color.Unspecified,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle based on tournament type
        Text(
            text =
                when (tournament.type) {
                    TournamentType.SMILEY -> stringResource(TournamentRes.string.smiley_subtitle)
                    TournamentType.HOT_OR_NOT -> stringResource(TournamentRes.string.hot_or_not_subtitle)
                },
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Meta row: schedule + participants (right-aligned)
        val scheduleLabel = scheduleText(tournament.status, tournament.startEpochMs, tournament.endEpochMs, now)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaChip(
                iconRes = TournamentRes.drawable.ic_calendar,
                iconTint = Color.Unspecified,
                text = scheduleLabel,
            )
            Spacer(modifier = Modifier.weight(1f))
            MetaChip(
                iconRes = TournamentRes.drawable.ic_users,
                iconTint = Color.Unspecified,
                text = tournament.participantsLabel,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prize row with ranking button (moved here from CTA row)
        if (tournament.status !is TournamentStatus.Ended) {
            PrizeRow(
                totalPrizePool = tournament.totalPrizePool,
                colors = colors,
                onRankingClick = onPrizeBreakdownClick,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // CTA button (full width, no ranking button beside it)
        TournamentCtaButton(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            status = tournament.status,
            participationState = currentParticipationState,
            onClick = onTournamentCtaClick,
            fillFraction = fillFraction,
            countdownText = countdownText,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun PrizeRow(
    totalPrizePool: Int,
    colors: TournamentCardColors,
    onRankingClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.prizeCardBg)
                    .border(width = 1.dp, color = colors.prizeCardBorder, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(TournamentRes.drawable.ic_trophy_cup),
                contentDescription = null,
            )
            Text(
                text = stringResource(TournamentRes.string.prize_label),
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextSecondary,
                modifier = Modifier.padding(start = 6.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(TournamentRes.string.win_upto_prize, totalPrizePool),
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.Yellow200,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Image(
                modifier = Modifier.padding(start = 4.dp).size(16.dp),
                painter = painterResource(TournamentRes.drawable.bitcoin),
                contentDescription = null,
            )
        }
        RankingIconButton(onClick = onRankingClick)
    }
}

@Suppress("MagicNumber")
internal fun formatRemainingDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    fun Long.twoDigits(): String = this.toString().padStart(2, '0')

    return when {
        hours > 0 -> "${hours.twoDigits()}:${minutes.twoDigits()}:${seconds.twoDigits()}"
        minutes > 0 -> "${minutes.twoDigits()}:${seconds.twoDigits()}"
        else -> "${minutes.twoDigits()}:${seconds.twoDigits()}"
    }
}

@Suppress("MagicNumber")
private fun formatHourMinute12h(
    hour: Int,
    minute: Int,
): String {
    val hour12 = ((hour + 11) % 12 + 1)
    val amPm = if (hour < 12) "AM" else "PM"
    return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
}

@Composable
private fun MetaChip(
    iconRes: DrawableResource,
    iconTint: Color,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
        )
        Text(
            text = text,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RankingIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(YralColors.Neutral950)
                .border(width = 1.dp, color = YralColors.Neutral700, shape = RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(TournamentRes.drawable.ic_ranking),
            contentDescription = "Prize breakdown",
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
    }
}

private data class TournamentCardColors(
    val headerBg: Color,
    val bodyBg: Color,
    val prizeCardBg: Color,
    val prizeCardBorder: Color,
)

@Suppress("MagicNumber")
private fun tournamentCardColors(type: TournamentType): TournamentCardColors =
    when (type) {
        TournamentType.SMILEY ->
            TournamentCardColors(
                headerBg = Color(0xFF231F12),
                bodyBg = Color(0xFF231F12),
                prizeCardBg = Color(0xFF17140C),
                prizeCardBorder = Color(0xFF342D1A),
            )
        TournamentType.HOT_OR_NOT ->
            TournamentCardColors(
                headerBg = Color(0xFF0D1C1A),
                bodyBg = Color(0xFF0D1C1A),
                prizeCardBg = Color(0xFF0A1413),
                prizeCardBorder = Color(0xFF162D2B),
            )
    }

/**
 * Derives the current participation state based on real-time, ensuring the button state
 * and countdown timer are always synchronized.
 */
@OptIn(ExperimentalTime::class)
private fun deriveParticipationState(
    tournament: Tournament,
    currentTime: Instant,
    proDetails: ProDetails,
): TournamentParticipationState {
    if (tournament.isDaily) {
        return deriveDailyParticipationState(tournament)
    }
    return deriveRegularParticipationState(tournament, currentTime, proDetails)
}

@OptIn(ExperimentalTime::class)
private fun deriveDailyParticipationState(tournament: Tournament): TournamentParticipationState {
    val remainingMs = tournament.remainingTimeMs ?: tournament.dailyTimeLimitMs
    return when {
        tournament.isRegistered && remainingMs <= 0 ->
            TournamentParticipationState.TimeExpired(tournament.userDiamonds)
        tournament.isRegistered ->
            TournamentParticipationState.JoinNow(tournament.userDiamonds)
        else -> TournamentParticipationState.JoinNowFree
    }
}

@OptIn(ExperimentalTime::class)
private fun deriveRegularParticipationState(
    tournament: Tournament,
    currentTime: Instant,
    proDetails: ProDetails,
): TournamentParticipationState {
    val startTime = Instant.fromEpochMilliseconds(tournament.startEpochMs)
    val endTime = Instant.fromEpochMilliseconds(tournament.endEpochMs)

    val isLive = currentTime in startTime..endTime
    val isUpcoming = currentTime < startTime
    val canUseCredit = proDetails.isProPurchased && proDetails.availableCredits >= tournament.entryCostCredits

    return when {
        tournament.isRegistered -> {
            when {
                isLive -> TournamentParticipationState.JoinNow(tournament.userDiamonds)
                isUpcoming -> {
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
        isLive ->
            if (canUseCredit) {
                TournamentParticipationState.JoinNowWithCredit(tournament.entryCostCredits)
            } else {
                TournamentParticipationState.JoinNowWithTokens(tournament.entryCost)
            }
        else ->
            if (canUseCredit) {
                TournamentParticipationState.JoinNowWithCredit(tournament.entryCostCredits)
            } else {
                TournamentParticipationState.RegistrationRequired(tournament.entryCost)
            }
    }
}

@Suppress("MagicNumber", "UnusedPrivateMember")
@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TournamentCardLivePreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentCard(
            tournament =
                Tournament(
                    id = "t_live",
                    title = "SMILY SHOWDOWN",
                    totalPrizePool = 10000,
                    participantsLabel = "32 Playing",
                    scheduleLabel = "Dec 4th • 6:00-6:30 pm",
                    status = TournamentStatus.Live(Clock.System.now() + 10.minutes),
                    participationState = TournamentParticipationState.RegistrationRequired(20),
                    prizeBreakdown = previewPrizeRows(),
                    startEpochMs = Clock.System.now().toEpochMilliseconds(),
                    endEpochMs = (Clock.System.now() + 10.minutes).toEpochMilliseconds(),
                    entryCost = 20,
                    entryCostCredits = 1,
                    isRegistered = false,
                    userDiamonds = 0,
                ),
            onShareClick = {},
            onTournamentCtaClick = {},
            onPrizeBreakdownClick = {},
            proDetails = ProDetails(isProPurchased = true, availableCredits = 0),
        )
    }
}

@Suppress("MagicNumber", "UnusedPrivateMember")
@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TournamentCardUpcomingPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentCard(
            tournament =
                Tournament(
                    id = "t_upcoming",
                    title = "SMILY SHOWDOWN",
                    totalPrizePool = 10000,
                    participantsLabel = "15 Registered",
                    scheduleLabel = "Dec 5th • 6:00-6:30 pm",
                    status = TournamentStatus.Upcoming(Clock.System.now() + 10.minutes),
                    participationState = TournamentParticipationState.RegistrationRequired(20),
                    prizeBreakdown = previewPrizeRows(),
                    startEpochMs = (Clock.System.now() + 20.minutes).toEpochMilliseconds(),
                    endEpochMs = (Clock.System.now() + 30.minutes).toEpochMilliseconds(),
                    entryCost = 20,
                    entryCostCredits = 1,
                    isRegistered = false,
                    userDiamonds = 0,
                ),
            onShareClick = {},
            onTournamentCtaClick = {},
            onPrizeBreakdownClick = {},
            proDetails = ProDetails(),
        )
    }
}

@Suppress("MagicNumber", "UnusedPrivateMember")
@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TournamentCardEndedPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentCard(
            tournament =
                Tournament(
                    id = "t_ended",
                    title = "SMILY SHOWDOWN",
                    totalPrizePool = 10000,
                    participantsLabel = "15 Participants",
                    scheduleLabel = "Dec 5th • 6:00-6:30 pm",
                    status = TournamentStatus.Ended,
                    participationState = TournamentParticipationState.RegistrationRequired(20),
                    prizeBreakdown = previewPrizeRows(),
                    startEpochMs = (Clock.System.now() - 20.minutes).toEpochMilliseconds(),
                    endEpochMs = (Clock.System.now() - 10.minutes).toEpochMilliseconds(),
                    entryCost = 20,
                    entryCostCredits = 1,
                    isRegistered = false,
                    userDiamonds = 0,
                ),
            onShareClick = {},
            onTournamentCtaClick = {},
            onPrizeBreakdownClick = {},
            proDetails = ProDetails(),
        )
    }
}

private fun previewPrizeRows(): List<PrizeBreakdownRow> =
    listOf(
        PrizeBreakdownRow(rank = 1, amount = 10000),
        PrizeBreakdownRow(rank = 2, amount = 5000),
        PrizeBreakdownRow(rank = 3, amount = 4000),
    )
