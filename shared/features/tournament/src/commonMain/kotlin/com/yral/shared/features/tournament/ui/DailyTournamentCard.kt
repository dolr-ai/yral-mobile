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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import com.yral.shared.libs.designsystem.theme.GradientAngleConvention
import com.yral.shared.libs.designsystem.theme.GradientLengthMode
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.angledGradientBackground
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.daily_card_daily
import yral_mobile.shared.features.tournament.generated.resources.daily_card_description_leaderboard
import yral_mobile.shared.features.tournament.generated.resources.daily_card_description_ready
import yral_mobile.shared.features.tournament.generated.resources.daily_card_description_waiting
import yral_mobile.shared.features.tournament.generated.resources.daily_card_free_game
import yral_mobile.shared.features.tournament.generated.resources.daily_card_game_live_in
import yral_mobile.shared.features.tournament.generated.resources.daily_card_play_for_free
import yral_mobile.shared.features.tournament.generated.resources.daily_card_play_game_in
import yral_mobile.shared.features.tournament.generated.resources.daily_card_play_in
import yral_mobile.shared.features.tournament.generated.resources.daily_card_restarts_in
import yral_mobile.shared.features.tournament.generated.resources.daily_card_title
import yral_mobile.shared.features.tournament.generated.resources.daily_tournament_emoji
import yral_mobile.shared.features.tournament.generated.resources.ic_question_circle
import yral_mobile.shared.features.tournament.generated.resources.ic_share
import yral_mobile.shared.features.tournament.generated.resources.ic_timer
import yral_mobile.shared.features.tournament.generated.resources.view_leaderboard
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import yral_mobile.shared.features.tournament.generated.resources.Res as TournamentRes

private sealed class DailyCardState {
    data object ReadyToPlay : DailyCardState()

    data class WaitingForRestart(
        val targetEpochMs: Long,
    ) : DailyCardState()

    data class GameLiveSoon(
        val targetEpochMs: Long,
    ) : DailyCardState()

    data class ViewLeaderboard(
        val restartTargetEpochMs: Long,
    ) : DailyCardState()
}

@Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalTime::class)
@Composable
fun DailyTournamentCard(
    tournament: Tournament,
    onShareClick: () -> Unit,
    onInfoClick: () -> Unit,
    onCtaClick: () -> Unit,
) {
    val now by produceState(initialValue = Clock.System.now(), tournament.status) {
        while (true) {
            value = Clock.System.now()
            delay(1.seconds)
        }
    }

    val cardState = deriveDailyCardState(tournament, now)

    val cardShape = RoundedCornerShape(8.dp)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .wrapContentHeight(),
    ) {
        // Header image section
        DailyCardHeader(
            cardState = cardState,
            now = now,
            onShareClick = onShareClick,
        )

        // Body section
        DailyCardBody(
            cardState = cardState,
            now = now,
            onInfoClick = onInfoClick,
            onCtaClick = onCtaClick,
        )
    }
}

@Suppress("MagicNumber")
@OptIn(ExperimentalTime::class)
@Composable
private fun DailyCardHeader(
    cardState: DailyCardState,
    now: Instant,
    onShareClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color(0xFF1C1223)),
    ) {
        // Banner emoji image
        Image(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            painter = painterResource(TournamentRes.drawable.daily_tournament_emoji),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )

        // Pill badge - top left
        DailyPillBadge(
            cardState = cardState,
            now = now,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp),
        )

        // Share icon - top right
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

@Suppress("MagicNumber")
@OptIn(ExperimentalTime::class)
@Composable
private fun DailyPillBadge(
    cardState: DailyCardState,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(999.dp)
    val pillBorderColor = Color(0xFF7B4D9A)
    val pillGradient =
        Brush.radialGradient(
            colors = listOf(Color(0xFF602986), Color(0xFF4A166F)),
        )

    val showTimer = cardState !is DailyCardState.ReadyToPlay
    val pillText = pillBadgeText(cardState, now)

    Row(
        modifier =
            modifier
                .clip(pillShape)
                .background(pillGradient)
                .border(width = 1.dp, color = pillBorderColor, shape = pillShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showTimer) {
            Icon(
                modifier = Modifier.size(12.dp),
                painter = painterResource(TournamentRes.drawable.ic_timer),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
        Text(
            text = pillText,
            style = LocalAppTopography.current.regSemiBold,
            color = YralColors.NeutralIconsActive,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun pillBadgeText(
    cardState: DailyCardState,
    now: Instant,
): String =
    when (cardState) {
        is DailyCardState.ReadyToPlay ->
            stringResource(TournamentRes.string.daily_card_free_game) +
                " | " +
                stringResource(TournamentRes.string.daily_card_daily)
        is DailyCardState.WaitingForRestart ->
            stringResource(
                TournamentRes.string.daily_card_restarts_in,
                formatRemainingDuration(Instant.fromEpochMilliseconds(cardState.targetEpochMs) - now),
            )
        is DailyCardState.GameLiveSoon ->
            stringResource(
                TournamentRes.string.daily_card_game_live_in,
                formatRemainingDuration(Instant.fromEpochMilliseconds(cardState.targetEpochMs) - now),
            )
        is DailyCardState.ViewLeaderboard ->
            stringResource(
                TournamentRes.string.daily_card_restarts_in,
                formatRemainingDuration(Instant.fromEpochMilliseconds(cardState.restartTargetEpochMs) - now),
            )
    }

@Suppress("MagicNumber", "LongMethod")
@OptIn(ExperimentalTime::class)
@Composable
private fun DailyCardBody(
    cardState: DailyCardState,
    now: Instant,
    onInfoClick: () -> Unit,
    onCtaClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1223))
                .padding(10.dp),
    ) {
        // Title row with info icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(TournamentRes.string.daily_card_title),
                style = LocalAppTopography.current.baseBold,
                color = YralColors.NeutralTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                modifier =
                    Modifier
                        .clickable(onClick = onInfoClick)
                        .padding(4.dp)
                        .size(20.dp),
                painter = painterResource(TournamentRes.drawable.ic_question_circle),
                contentDescription = "How to play",
                tint = Color.Unspecified,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Description
        Text(
            text = dailyCardDescription(cardState),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CTA Button
        DailyCtaButton(
            cardState = cardState,
            now = now,
            onCtaClick = onCtaClick,
        )
    }
}

@Composable
private fun dailyCardDescription(cardState: DailyCardState): String =
    when (cardState) {
        is DailyCardState.ReadyToPlay ->
            stringResource(TournamentRes.string.daily_card_description_ready)
        is DailyCardState.WaitingForRestart ->
            stringResource(TournamentRes.string.daily_card_description_waiting)
        is DailyCardState.GameLiveSoon ->
            stringResource(TournamentRes.string.daily_card_description_ready)
        is DailyCardState.ViewLeaderboard ->
            stringResource(TournamentRes.string.daily_card_description_leaderboard)
    }

@Suppress("MagicNumber", "LongMethod")
@OptIn(ExperimentalTime::class)
@Composable
private fun DailyCtaButton(
    cardState: DailyCardState,
    now: Instant,
    onCtaClick: () -> Unit,
) {
    val buttonShape = RoundedCornerShape(8.dp)

    when (cardState) {
        is DailyCardState.ReadyToPlay -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(buttonShape)
                        .angledGradientBackground(
                            degrees = 218f,
                            colorStops = tournamentPinkGradientStops(),
                            angleConvention = GradientAngleConvention.CssDegrees,
                            lengthMode = GradientLengthMode.Diagonal,
                        ).clickable(onClick = onCtaClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(TournamentRes.string.daily_card_play_for_free),
                    style = LocalAppTopography.current.baseSemiBold,
                    color = YralColors.NeutralIconsActive,
                )
            }
        }

        is DailyCardState.WaitingForRestart -> {
            val timeRemainingMs =
                (Instant.fromEpochMilliseconds(cardState.targetEpochMs) - now)
                    .inWholeMilliseconds
                    .coerceAtLeast(0)
            val gradientFraction =
                (1f - (timeRemainingMs.toFloat() / TWENTY_FOUR_HOURS_MS))
                    .coerceIn(0f, 1f)

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(buttonShape),
                contentAlignment = Alignment.Center,
            ) {
                // Layer 1: Dark pink background
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF71003D), buttonShape),
                )
                // Layer 2: Pink gradient fill (progress)
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = gradientFraction)
                            .align(Alignment.CenterStart)
                            .angledGradientBackground(
                                degrees = 218f,
                                colorStops = tournamentPinkGradientStops(),
                                angleConvention = GradientAngleConvention.CssDegrees,
                                lengthMode = GradientLengthMode.Diagonal,
                            ),
                )
                // Layer 3: Text
                Text(
                    text =
                        stringResource(
                            TournamentRes.string.daily_card_play_in,
                            formatRemainingDuration(
                                Instant.fromEpochMilliseconds(cardState.targetEpochMs) - now,
                            ),
                        ),
                    style = LocalAppTopography.current.baseSemiBold,
                    color = YralColors.Pink100,
                )
            }
        }

        is DailyCardState.GameLiveSoon -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(buttonShape)
                        .angledGradientBackground(
                            degrees = 218f,
                            colorStops = tournamentPinkGradientStops(),
                            angleConvention = GradientAngleConvention.CssDegrees,
                            lengthMode = GradientLengthMode.Diagonal,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        stringResource(
                            TournamentRes.string.daily_card_play_game_in,
                            formatRemainingDuration(
                                Instant.fromEpochMilliseconds(cardState.targetEpochMs) - now,
                            ),
                        ),
                    style = LocalAppTopography.current.baseSemiBold,
                    color = YralColors.NeutralIconsActive,
                )
            }
        }

        is DailyCardState.ViewLeaderboard -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(buttonShape)
                        .background(Color.White)
                        .clickable(onClick = onCtaClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(TournamentRes.string.view_leaderboard),
                    style =
                        LocalAppTopography.current.baseSemiBold.copy(
                            brush =
                                Brush.linearGradient(
                                    colorStops = tournamentPinkGradientStops(),
                                ),
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun deriveDailyCardState(
    tournament: Tournament,
    now: Instant,
): DailyCardState {
    val isTimeExpired =
        tournament.isRegistered &&
            (tournament.remainingTimeMs ?: tournament.dailyTimeLimitMs) <= 0

    return when {
        isTimeExpired -> DailyCardState.ViewLeaderboard(tournament.endEpochMs)
        tournament.status is TournamentStatus.Upcoming -> {
            val startTime = Instant.fromEpochMilliseconds(tournament.startEpochMs)
            val timeToStart = startTime - now
            if (timeToStart <= TEN_MINUTES) {
                DailyCardState.GameLiveSoon(tournament.startEpochMs)
            } else {
                DailyCardState.WaitingForRestart(tournament.startEpochMs)
            }
        }
        else -> DailyCardState.ReadyToPlay
    }
}

private val TEN_MINUTES = 10.minutes
private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000f

@Suppress("MagicNumber", "UnusedPrivateMember")
@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun DailyTournamentCardReadyPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        DailyTournamentCard(
            tournament =
                previewDailyTournament(
                    status = TournamentStatus.Live(Clock.System.now() + 30.minutes),
                    isRegistered = false,
                ),
            onShareClick = {},
            onInfoClick = {},
            onCtaClick = {},
        )
    }
}

@Suppress("MagicNumber", "UnusedPrivateMember")
@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun DailyTournamentCardWaitingPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        DailyTournamentCard(
            tournament =
                previewDailyTournament(
                    status = TournamentStatus.Upcoming(Clock.System.now() + 120.minutes),
                    isRegistered = false,
                ),
            onShareClick = {},
            onInfoClick = {},
            onCtaClick = {},
        )
    }
}

@Suppress("MagicNumber", "UnusedPrivateMember")
@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun DailyTournamentCardLiveSoonPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        DailyTournamentCard(
            tournament =
                previewDailyTournament(
                    status = TournamentStatus.Upcoming(Clock.System.now() + 5.minutes),
                    isRegistered = false,
                ),
            onShareClick = {},
            onInfoClick = {},
            onCtaClick = {},
        )
    }
}

@Suppress("MagicNumber", "UnusedPrivateMember")
@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun DailyTournamentCardViewLeaderboardPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        DailyTournamentCard(
            tournament =
                previewDailyTournament(
                    status = TournamentStatus.Live(Clock.System.now() + 30.minutes),
                    isRegistered = true,
                    remainingTimeMs = 0,
                ),
            onShareClick = {},
            onInfoClick = {},
            onCtaClick = {},
        )
    }
}

@Suppress("MagicNumber")
@OptIn(ExperimentalTime::class)
private fun previewDailyTournament(
    status: TournamentStatus,
    isRegistered: Boolean,
    remainingTimeMs: Long? = null,
): Tournament =
    Tournament(
        id = "daily_preview",
        title = "Free 5-Minute Smily Game",
        totalPrizePool = 0,
        participantsLabel = "42 Playing",
        scheduleLabel = "Daily",
        status = status,
        participationState = com.yral.shared.features.tournament.domain.model.TournamentParticipationState.JoinNowFree,
        prizeBreakdown = emptyList(),
        startEpochMs = (Clock.System.now() + 30.minutes).toEpochMilliseconds(),
        endEpochMs = (Clock.System.now() + 60.minutes).toEpochMilliseconds(),
        entryCost = 0,
        entryCostCredits = 0,
        isRegistered = isRegistered,
        userDiamonds = 0,
        isDaily = true,
        dailyTimeLimitMs = 300_000,
        remainingTimeMs = remainingTimeMs,
    )
