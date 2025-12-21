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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.features.tournament.domain.model.PrizeBreakdownRow
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import com.yral.shared.libs.designsystem.modifierx.conditional
import com.yral.shared.libs.designsystem.modifierx.grayScale
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.angledGradientBackground
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.bitcoin
import yral_mobile.shared.features.tournament.generated.resources.dela_gothic_one_regular
import yral_mobile.shared.features.tournament.generated.resources.ended
import yral_mobile.shared.features.tournament.generated.resources.ends_in
import yral_mobile.shared.features.tournament.generated.resources.hourly_tournament_emoji
import yral_mobile.shared.features.tournament.generated.resources.ic_calendar
import yral_mobile.shared.features.tournament.generated.resources.ic_ranking
import yral_mobile.shared.features.tournament.generated.resources.ic_share
import yral_mobile.shared.features.tournament.generated.resources.ic_timer
import yral_mobile.shared.features.tournament.generated.resources.ic_users
import yral_mobile.shared.features.tournament.generated.resources.starts_in
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import yral_mobile.shared.features.tournament.generated.resources.Res as TournamentRes

@Suppress("MagicNumber", "LongMethod")
@Composable
fun TournamentCard(
    tournament: Tournament,
    onPrizeBreakdownClick: () -> Unit,
    onShareClick: () -> Unit,
    onTournamentCtaClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(8.dp)

    val gradientColor =
        when (tournament.status) {
            TournamentStatus.Ended -> Color(0xFF878787)
            is TournamentStatus.Live -> YralColors.Red400
            is TournamentStatus.Upcoming -> YralColors.Green400
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .wrapContentHeight()
                .angledGradientBackground(
                    colorStops =
                        arrayOf(
                            0.5141f to YralColors.Neutral900,
                            1.5421f to gradientColor,
                        ),
                    degrees = 131f,
                ),
    ) {
        StatusChip(
            status = tournament.status,
            modifier = Modifier.align(Alignment.TopStart),
        )

        if (tournament.status !is TournamentStatus.Ended) {
            ShareIcon(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .clickable(onClick = onShareClick)
                        .padding(top = 16.dp, end = 20.dp),
            )
        }

        Row {
            Column(
                modifier =
                    Modifier
                        .padding(start = 12.dp, end = 12.dp, top = 44.dp)
                        .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = tournament.title,
                    style =
                        TextStyle(
                            fontFamily = delaGothicOneFontFamily(),
                            fontWeight = FontWeight.Normal,
                            fontSize = titleSize(tournament.status),
                            letterSpacing = titleLetterSpacing(tournament.status),
                            lineHeight = titleLineHeight(tournament.status),
                            color = YralColors.NeutralTextPrimary,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tournament.status !is TournamentStatus.Ended) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = tournament.subtitle,
                            style = LocalAppTopography.current.xlBold,
                            color = YralColors.Yellow200,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Image(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .graphicsLayer(rotationZ = 3.919f),
                            painter = painterResource(TournamentRes.drawable.bitcoin),
                            contentDescription = null,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip(
                        iconRes = TournamentRes.drawable.ic_users,
                        iconTint = Color.Unspecified,
                        alpha = 0.7f,
                        cornerRadius = 64.dp,
                        text = tournament.participantsLabel,
                    )
                    MetaChip(
                        iconRes = TournamentRes.drawable.ic_calendar,
                        iconTint = Color.Unspecified,
                        alpha = 0.8f,
                        cornerRadius = 30.dp,
                        text = tournament.scheduleLabel,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TournamentCtaButton(
                        modifier =
                            Modifier
                                .height(40.dp)
                                .weight(1f),
                        status = tournament.status,
                        participationState = tournament.participationState,
                        onClick = onTournamentCtaClick,
                    )
                    if (tournament.status !is TournamentStatus.Ended) {
                        RankingIconButton(onClick = onPrizeBreakdownClick)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Image(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(100.dp)
                    .conditional(
                        condition = tournament.status is TournamentStatus.Ended,
                        ifTrue = { grayScale() },
                    ),
            painter = painterResource(TournamentRes.drawable.hourly_tournament_emoji),
            contentDescription = null,
        )
    }
}

@Composable
private fun StatusChip(
    status: TournamentStatus,
    modifier: Modifier = Modifier,
) {
    val bg =
        when (status) {
            is TournamentStatus.Live -> YralColors.Red400
            is TournamentStatus.Upcoming -> YralColors.Green400
            TournamentStatus.Ended -> YralColors.Neutral700
        }

    val label =
        when (status) {
            is TournamentStatus.Live -> stringResource(TournamentRes.string.starts_in, "temp")
            is TournamentStatus.Upcoming -> stringResource(TournamentRes.string.ends_in, "temp")
            TournamentStatus.Ended -> stringResource(TournamentRes.string.ended)
        }

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(bottomEnd = 12.dp))
                .background(bg)
                .height(28.dp)
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(TournamentRes.drawable.ic_timer),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Text(
            text = label,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.NeutralIconsActive,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetaChip(
    iconRes: DrawableResource,
    iconTint: Color,
    alpha: Float,
    cornerRadius: Dp,
    text: String,
) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(YralColors.Neutral800.copy(alpha = alpha))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
private fun ShareIcon(modifier: Modifier = Modifier) {
    Icon(
        modifier =
            modifier
                .size(20.dp),
        painter = painterResource(TournamentRes.drawable.ic_share),
        contentDescription = "Share",
        tint = Color.Unspecified,
    )
}

@Composable
private fun RankingIconButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
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

@Composable
private fun delaGothicOneFontFamily(): FontFamily =
    FontFamily(
        Font(TournamentRes.font.dela_gothic_one_regular, FontWeight.Normal),
    )

private fun titleSize(status: TournamentStatus) =
    when (status) {
        TournamentStatus.Ended -> 18.sp
        else -> 14.sp
    }

private fun titleLetterSpacing(status: TournamentStatus) =
    when (status) {
        TournamentStatus.Ended -> 0.54.sp
        else -> 0.42.sp
    }

private fun titleLineHeight(status: TournamentStatus) =
    when (status) {
        TournamentStatus.Ended -> 25.2.sp
        else -> 19.6.sp
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
                    subtitle = "Win up to ₹10,000 in",
                    participantsLabel = "32 Playing",
                    scheduleLabel = "Dec 4th • 6:00-6:30 pm",
                    status = TournamentStatus.Live(Clock.System.now() + 10.minutes),
                    participationState = TournamentParticipationState.RegistrationRequired(20),
                    prizeBreakdown = previewPrizeRows(),
                ),
            onShareClick = {},
            onTournamentCtaClick = {},
            onPrizeBreakdownClick = {},
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
                    subtitle = "Win up to ₹10,000 in",
                    participantsLabel = "15 Registered",
                    scheduleLabel = "Dec 5th • 6:00-6:30 pm",
                    status = TournamentStatus.Upcoming(Clock.System.now() + 10.minutes),
                    participationState = TournamentParticipationState.RegistrationRequired(20),
                    prizeBreakdown = previewPrizeRows(),
                ),
            onShareClick = {},
            onTournamentCtaClick = {},
            onPrizeBreakdownClick = {},
        )
    }
}

@Suppress("MagicNumber", "UnusedPrivateMember")
@Preview
@Composable
private fun TournamentCardEndedPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentCard(
            tournament =
                Tournament(
                    id = "t_ended",
                    title = "SMILY SHOWDOWN",
                    subtitle = "Win up to ₹10,000 in",
                    participantsLabel = "15 Participants",
                    scheduleLabel = "Dec 5th • 6:00-6:30 pm",
                    status = TournamentStatus.Ended,
                    participationState = TournamentParticipationState.RegistrationRequired(20),
                    prizeBreakdown = previewPrizeRows(),
                ),
            onShareClick = {},
            onTournamentCtaClick = {},
            onPrizeBreakdownClick = {},
        )
    }
}

private fun previewPrizeRows(): List<PrizeBreakdownRow> =
    listOf(
        PrizeBreakdownRow(rankLabel = "1st Place", amountLabel = "₹10,000 worth of"),
        PrizeBreakdownRow(rankLabel = "2nd Place", amountLabel = "₹5,000 in"),
        PrizeBreakdownRow(rankLabel = "3rd Place", amountLabel = "₹4,000 in"),
    )
