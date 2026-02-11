package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import com.yral.shared.libs.designsystem.theme.GradientAngleConvention
import com.yral.shared.libs.designsystem.theme.GradientLengthMode
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.angledGradientBackground
import com.yral.shared.libs.designsystem.theme.linearGradientBrush
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.check_tick
import yral_mobile.shared.features.tournament.generated.resources.join_now
import yral_mobile.shared.features.tournament.generated.resources.join_now_with_tokens
import yral_mobile.shared.features.tournament.generated.resources.join_with_credit
import yral_mobile.shared.features.tournament.generated.resources.register_with_tokens
import yral_mobile.shared.features.tournament.generated.resources.registered
import yral_mobile.shared.features.tournament.generated.resources.view_leaderboard
import yral_mobile.shared.features.tournament.generated.resources.yral_coin
import yral_mobile.shared.libs.designsystem.generated.resources.ic_thunder
import yral_mobile.shared.libs.designsystem.generated.resources.pink_gradient_background_disabled
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun TournamentCtaButton(
    modifier: Modifier = Modifier.Companion,
    status: TournamentStatus,
    participationState: TournamentParticipationState,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier =
            modifier
                .clip(shape)
                .then(
                    when (status) {
                        is TournamentStatus.Ended ->
                            Modifier
                                .background(YralColors.Neutral50)
                                .border(1.dp, YralColors.Neutral700, shape)

                        else ->
                            when (participationState) {
                                is TournamentParticipationState.JoinNow,
                                is TournamentParticipationState.JoinNowWithTokens,
                                is TournamentParticipationState.JoinNowWithCredit,
                                -> {
                                    Modifier.angledGradientBackground(
                                        degrees = 218f,
                                        colorStops = tournamentPinkGradientStops(),
                                        angleConvention = GradientAngleConvention.CssDegrees,
                                        lengthMode = GradientLengthMode.Diagonal,
                                    )
                                }
                                TournamentParticipationState.JoinNowDisabled -> {
                                    Modifier.paint(
                                        painter = painterResource(DesignRes.drawable.pink_gradient_background_disabled),
                                        contentScale = ContentScale.FillBounds,
                                    )
                                }

                                TournamentParticipationState.Registered -> {
                                    Modifier.Companion
                                        .background(YralColors.Neutral700)
                                        .border(1.dp, YralColors.Neutral700, shape)
                                }

                                is TournamentParticipationState.RegistrationRequired -> {
                                    Modifier.Companion
                                        .background(YralColors.Neutral50)
                                        .border(1.dp, YralColors.Neutral700, shape)
                                }
                            }
                    },
                ).clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Companion.Center,
    ) {
        Row(
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (status != TournamentStatus.Ended && participationState is TournamentParticipationState.Registered) {
                Image(
                    modifier = Modifier.Companion.size(20.dp),
                    painter = painterResource(Res.drawable.check_tick),
                    contentDescription = null,
                )
            }

            val labelStyle =
                LocalAppTopography.current.baseSemiBold.copy(
                    color = Color.Companion.Unspecified,
                )

            val labelBrush =
                when (status) {
                    TournamentStatus.Ended -> tournamentPinkGradientBrush(angleDegrees = 198.93394f)
                    else -> {
                        when (participationState) {
                            is TournamentParticipationState.RegistrationRequired -> {
                                tournamentPinkGradientBrush(angleDegrees = 188.3126f)
                            }

                            else -> null
                        }
                    }
                }

            Text(
                text = ctaLabel(status, participationState),
                style =
                    if (labelBrush == null) {
                        val color =
                            if (participationState is TournamentParticipationState.JoinNowDisabled) {
                                YralColors.Pink100
                            } else {
                                YralColors.NeutralIconsActive
                            }
                        labelStyle.copy(color = color)
                    } else {
                        labelStyle.copy(brush = labelBrush)
                    },
                maxLines = 1,
                overflow = TextOverflow.Companion.Ellipsis,
            )

            if (status != TournamentStatus.Ended) {
                val iconRes =
                    when (participationState) {
                        is TournamentParticipationState.JoinNowWithCredit ->
                            DesignRes.drawable.ic_thunder
                        is TournamentParticipationState.RegistrationRequired,
                        is TournamentParticipationState.JoinNowWithTokens,
                        -> Res.drawable.yral_coin
                        else -> null
                    }
                if (iconRes != null) {
                    Image(
                        modifier = Modifier.size(width = 15.dp, height = 16.dp),
                        painter = painterResource(iconRes),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ctaLabel(
    status: TournamentStatus,
    participationState: TournamentParticipationState,
): String =
    when (status) {
        TournamentStatus.Ended -> stringResource(Res.string.view_leaderboard)
        else ->
            when (participationState) {
                is TournamentParticipationState.Registered -> stringResource(Res.string.registered)
                is TournamentParticipationState.JoinNow -> stringResource(Res.string.join_now)
                is TournamentParticipationState.JoinNowWithTokens ->
                    stringResource(
                        Res.string.join_now_with_tokens,
                        participationState.tokensRequired,
                    )
                is TournamentParticipationState.JoinNowWithCredit ->
                    stringResource(
                        Res.string.join_with_credit,
                        participationState.creditsRequired,
                    )
                is TournamentParticipationState.JoinNowDisabled -> stringResource(Res.string.join_now)
                is TournamentParticipationState.RegistrationRequired ->
                    stringResource(
                        Res.string.register_with_tokens,
                        participationState.tokensRequired,
                    )
            }
    }

private fun tournamentPinkGradientBrush(angleDegrees: Float): Brush? =
    linearGradientBrush(
        angleDegrees = angleDegrees,
        size = Size(width = 240f, height = 40f),
        colorStops = tournamentPinkGradientStops(),
        angleConvention = GradientAngleConvention.CssDegrees,
        lengthMode = GradientLengthMode.Diagonal,
    )

@Suppress("MagicNumber")
private fun tournamentPinkGradientStops(): Array<Pair<Float, Color>> =
    arrayOf(
        0.0983f to Color(0xFFFF78C1),
        0.4479f to YralColors.Pink300,
        0.7848f to Color(0xFFAD005E),
    )
