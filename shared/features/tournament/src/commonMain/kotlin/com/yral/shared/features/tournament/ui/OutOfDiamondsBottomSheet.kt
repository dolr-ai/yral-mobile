package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.features.tournament.domain.model.TournamentParticipationState
import com.yral.shared.features.tournament.domain.model.TournamentStatus
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.out_of_diamonds_exit_anyway
import yral_mobile.shared.features.tournament.generated.resources.out_of_diamonds_message
import yral_mobile.shared.features.tournament.generated.resources.out_of_diamonds_title
import yral_mobile.shared.features.tournament.generated.resources.tournament_out_of_diamonds
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
internal fun OutOfDiamondsBottomSheet(
    nextTournamentStartTime: Instant,
    tokensRequired: Int,
    onDismissRequest: () -> Unit,
    onRegisterNowClick: () -> Unit,
    onExitAnywayClick: () -> Unit,
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
                    .padding(horizontal = 16.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(width = 100.dp, height = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.tournament_out_of_diamonds),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(Res.string.out_of_diamonds_title),
                style = LocalAppTopography.current.lgBold,
                color = YralColors.NeutralTextPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text =
                    stringResource(
                        Res.string.out_of_diamonds_message,
                        nextTournamentStartTime.toString(),
                    ),
                style = LocalAppTopography.current.mdRegular,
                color = YralColors.Neutral300,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(40.dp))
            RegisterNowButton(
                nextTournamentStartTime = nextTournamentStartTime,
                tokensRequired = tokensRequired,
                onClick = onRegisterNowClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ExitAnywayButton(onClick = onExitAnywayClick)
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun RegisterNowButton(
    nextTournamentStartTime: Instant,
    tokensRequired: Int,
    onClick: () -> Unit,
) {
    TournamentCtaButton(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        status = TournamentStatus.Upcoming(nextTournamentStartTime),
        participationState = TournamentParticipationState.RegistrationRequired(tokensRequired),
        onClick = onClick,
    )
}

@Composable
private fun ExitAnywayButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(YralColors.Neutral800, shape)
                .border(1.dp, YralColors.Neutral700, shape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.out_of_diamonds_exit_anyway),
            style = LocalAppTopography.current.mdMedium,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@OptIn(ExperimentalTime::class)
@Suppress("UnusedPrivateMember", "MagicNumber")
@Preview
@Composable
private fun OutOfDiamondsBottomSheetPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        OutOfDiamondsBottomSheet(
            nextTournamentStartTime = Clock.System.now() + 10.minutes,
            tokensRequired = 20,
            onDismissRequest = {},
            onRegisterNowClick = {},
            onExitAnywayClick = {},
        )
    }
}
