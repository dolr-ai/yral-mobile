package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.features.tournament.domain.model.PrizeBreakdownRow
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import com.yral.shared.features.tournament.domain.model.getOrdinalSuffix
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.modifierx.conditional
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.title_price_breakdown
import yral_mobile.shared.libs.designsystem.generated.resources.bitcoin
import yral_mobile.shared.libs.designsystem.generated.resources.victory_cup
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrizeBreakdownBottomSheet(
    rows: List<PrizeBreakdownRow>,
    status: TournamentStatus,
    participationState: TournamentParticipationState,
    onDismissRequest: () -> Unit,
    onCtaClicked: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier.size(80.dp),
                painter = painterResource(DesignRes.drawable.victory_cup),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
            )
            Spacer(modifier = Modifier.height(19.dp))

            Text(
                text = stringResource(Res.string.title_price_breakdown),
                style = LocalAppTopography.current.xlBold,
                color = YralColors.Yellow200,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text =
                    buildAnnotatedString {
                        append("Top ${rows.count()} players win ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("BTC rewards worth up to ₹${rows.maxOf { it.amount }}.")
                        }
                    },
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.Neutral200,
            )

            Spacer(modifier = Modifier.height(30.dp))

            Column {
                rows.forEachIndexed { index, row ->
                    PrizeRow(
                        row = row,
                        isHighlighted = index == 0,
                    )
                    if (index != rows.lastIndex) {
                        HorizontalDivider(color = YralColors.Neutral700)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            TournamentCtaButton(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                status = status,
                participationState = participationState,
                onClick = onCtaClicked,
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun PrizeRow(
    row: PrizeBreakdownRow,
    isHighlighted: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .conditional(
                    isHighlighted,
                    ifTrue = {
                        Modifier.background(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            Color(0xFFBF760B),
                                            Color(0xFFFFE89F),
                                            Color(0xFFC38F00),
                                        ),
                                ),
                            shape = RoundedCornerShape(8.dp),
                        )
                    },
                ).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val textColor = if (isHighlighted) YralColors.Red400 else YralColors.NeutralTextPrimary
        Text(
            text = formatRankLabel(row.rank),
            style = LocalAppTopography.current.baseBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "₹${row.amount}",
                style = LocalAppTopography.current.baseBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(DesignRes.drawable.bitcoin),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun formatRankLabel(rank: Int): String = "$rank${getOrdinalSuffix(rank)} Place"

@OptIn(ExperimentalTime::class)
@Suppress("UnusedPrivateMember", "MagicNumber")
@Preview
@Composable
private fun PrizeBreakdownBottomSheetPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        PrizeBreakdownBottomSheet(
            rows =
                listOf(
                    PrizeBreakdownRow(rank = 1, amount = 10000),
                    PrizeBreakdownRow(rank = 2, amount = 5000),
                    PrizeBreakdownRow(rank = 3, amount = 4000),
                    PrizeBreakdownRow(rank = 4, amount = 3000),
                    PrizeBreakdownRow(rank = 5, amount = 2000),
                    PrizeBreakdownRow(rank = 6, amount = 1000),
                    PrizeBreakdownRow(rank = 7, amount = 500),
                    PrizeBreakdownRow(rank = 8, amount = 400),
                    PrizeBreakdownRow(rank = 9, amount = 300),
                    PrizeBreakdownRow(rank = 10, amount = 100),
                ),
            status = TournamentStatus.Upcoming(Clock.System.now() + 10.minutes),
            participationState = TournamentParticipationState.RegistrationRequired(20),
            onCtaClicked = {},
            onDismissRequest = {},
        )
    }
}
