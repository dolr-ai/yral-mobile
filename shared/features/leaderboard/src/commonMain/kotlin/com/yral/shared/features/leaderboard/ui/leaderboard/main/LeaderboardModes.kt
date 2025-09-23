package com.yral.shared.features.leaderboard.ui.leaderboard.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.features.leaderboard.data.models.LeaderboardMode
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardMainScreenConstants.COUNT_DOWN_ANIMATION_DURATION
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardMainScreenConstants.COUNT_DOWN_BG_ALPHA
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardMainScreenConstants.COUNT_DOWN_BORDER_ANIMATION_DURATION
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.component.YralNeonBorder
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.leaderboard.generated.resources.Res
import yral_mobile.shared.features.leaderboard.generated.resources.all_wins
import yral_mobile.shared.features.leaderboard.generated.resources.daily_wins
import yral_mobile.shared.features.leaderboard.generated.resources.end_in
import yral_mobile.shared.features.leaderboard.generated.resources.ic_calander_week_1
import yral_mobile.shared.features.leaderboard.generated.resources.ic_clock
import yral_mobile.shared.features.leaderboard.generated.resources.ic_gradient_clock
import yral_mobile.shared.libs.designsystem.generated.resources.count_down_timer
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun LeaderboardModeSelection(
    selectedMode: LeaderboardMode,
    selectMode: (LeaderboardMode) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .height(44.dp)
                .background(color = YralColors.Neutral950, shape = RoundedCornerShape(size = 45.dp))
                .padding(4.dp),
    ) {
        LeaderMode(
            modifier = Modifier.weight(1f),
            selectedMode = selectedMode,
            selectMode = selectMode,
            mode = LeaderboardMode.DAILY,
        )
        LeaderMode(
            modifier = Modifier.weight(1f),
            selectedMode = selectedMode,
            selectMode = selectMode,
            mode = LeaderboardMode.ALL_TIME,
        )
    }
}

@Composable
private fun LeaderMode(
    modifier: Modifier,
    mode: LeaderboardMode,
    selectedMode: LeaderboardMode,
    selectMode: (LeaderboardMode) -> Unit,
) {
    val modeText =
        when (mode) {
            LeaderboardMode.DAILY -> stringResource(Res.string.daily_wins)
            LeaderboardMode.ALL_TIME -> stringResource(Res.string.all_wins)
        }
    val chipColor =
        when (selectedMode == mode) {
            true -> YralColors.NeutralIconsActive
            false -> YralColors.Neutral950
        }
    val chipTextColor =
        when (selectedMode == mode) {
            true -> YralColors.Neutral950
            false -> YralColors.NeutralTextTertiary
        }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .height(36.dp)
                .background(color = chipColor, shape = RoundedCornerShape(size = 32.dp))
                .padding(4.dp)
                .clickable { selectMode(mode) },
    ) {
        Text(
            text = modeText,
            style = LocalAppTopography.current.xlBold,
            color = chipTextColor,
        )
    }
}

@Suppress("LongMethod")
@Composable
fun ColumnScope.LeaderboardCountdown(
    countDownMs: Long,
    blinkCountDown: Boolean,
) {
    var neon by remember(blinkCountDown) { mutableStateOf(false) }
    LaunchedEffect(blinkCountDown) {
        if (blinkCountDown) {
            while (true) {
                delay(COUNT_DOWN_ANIMATION_DURATION.toLong())
                neon = !neon
            }
        }
    }
    Box(Modifier.align(Alignment.CenterHorizontally)) {
        var showWhiteBorder by remember(blinkCountDown) { mutableStateOf(false) }
        if (blinkCountDown) {
            LeaderboardCountdownBorder(YralColors.Pink300)
            if (showWhiteBorder) {
                LeaderboardCountdownBorder(Color.White)
            }
        }
        LaunchedEffect(blinkCountDown) {
            if (blinkCountDown) {
                delay(COUNT_DOWN_ANIMATION_DURATION.toLong())
                showWhiteBorder = true
            }
        }
        Box {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .height(26.dp)
                        .width(115.dp)
                        .alpha(COUNT_DOWN_BG_ALPHA)
                        .background(
                            color = YralColors.Neutral950,
                            shape = RoundedCornerShape(size = 39.dp),
                        ).padding(start = 4.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
            ) {
                AnimatedContent(
                    targetState = neon,
                    transitionSpec = {
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = COUNT_DOWN_ANIMATION_DURATION / 2,
                                    easing = FastOutLinearInEasing,
                                ),
                        ) togetherWith
                            fadeOut(
                                animationSpec =
                                    tween(
                                        durationMillis = COUNT_DOWN_ANIMATION_DURATION / 2,
                                        easing = FastOutLinearInEasing,
                                    ),
                            )
                    },
                    label = "Leaderboard countdown",
                ) { target -> LeaderboardCountdownContent(target, countDownMs) }
            }
        }
    }
}

@Composable
private fun BoxScope.LeaderboardCountdownBorder(borderColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteBounce")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                tween(
                    durationMillis = COUNT_DOWN_BORDER_ANIMATION_DURATION,
                    easing = FastOutLinearInEasing,
                ),
                RepeatMode.Reverse,
            ),
        label = "alpha",
    )
    val containerColor = YralColors.Neutral950
    val cornerRadius = 49.dp
    val paddingValues = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
    val neonColor = borderColor
    YralNeonBorder(
        paddingValues = paddingValues,
        cornerRadius = cornerRadius,
        containerColor = containerColor,
        neonColor = neonColor,
        borderWidth = 10f,
        animationDuration = COUNT_DOWN_BORDER_ANIMATION_DURATION.toLong(),
    )
}

@Composable
private fun LeaderboardCountdownContent(
    isNeon: Boolean,
    countDownMs: Long,
) {
    val time = formatMillisToHHmmSS(countDownMs)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isNeon) {
            Image(
                painter = painterResource(Res.drawable.ic_clock),
                contentDescription = "image description",
                contentScale = ContentScale.None,
            )
            Text(
                text = stringResource(Res.string.end_in, time),
                style = LocalAppTopography.current.regBold,
                color = YralColors.Neutral50,
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.ic_gradient_clock),
                contentDescription = "image description",
                contentScale = ContentScale.None,
            )
            YralMaskedVectorTextV2(
                text = stringResource(Res.string.end_in, time),
                drawableRes = DesignRes.drawable.count_down_timer,
                textStyle = LocalAppTopography.current.regBold,
            )
        }
    }
}

@Composable
fun LeaderboardHistoryIcon(modifier: Modifier) {
    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .size(44.dp)
                    .background(
                        color = YralColors.ScrimColorIcon,
                        shape = RoundedCornerShape(size = 73.dp),
                    ),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_calander_week_1),
                contentDescription = "image description",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

internal expect fun formatMillisToHHmmSS(millis: Long): String
