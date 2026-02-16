package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.countdown_get_ready
import yral_mobile.shared.features.tournament.generated.resources.sand_timer
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("MagicNumber", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun TournamentCountdownBottomSheet(
    tournament: Tournament,
    onDismissRequest: () -> Unit,
    onCountdownFinished: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val now by produceState(initialValue = Clock.System.now()) {
        while (true) {
            value = Clock.System.now()
            delay(1.seconds)
        }
    }

    val startTime = Instant.fromEpochMilliseconds(tournament.startEpochMs)
    val remainingDuration = startTime - now

    LaunchedEffect(remainingDuration <= Duration.ZERO) {
        if (remainingDuration <= Duration.ZERO) {
            onCountdownFinished()
        }
    }

    val containerBrush =
        Brush.verticalGradient(
            colorStops =
                arrayOf(
                    0f to Color(0xFF1A1400),
                    1f to Color(0xFF0D0A00),
                ),
        )

    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(containerBrush)
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DragHandle()
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(Res.drawable.sand_timer),
                contentDescription = null,
                modifier = Modifier.size(150.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.countdown_get_ready, tournament.title),
                style = LocalAppTopography.current.lgMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            CountdownPill(remainingDuration)
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun CountdownPill(remainingDuration: Duration) {
    val pillShape = RoundedCornerShape(999.dp)
    Box(
        modifier =
            Modifier
                .clip(pillShape)
                .background(Color(0x66342911))
                .border(
                    width = 1.dp,
                    color = Color(0xFF473715),
                    shape = pillShape,
                ).padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatRemainingDuration(remainingDuration),
            color = Color(0xFFFFDF27),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(YralColors.Neutral500),
        )
    }
}
