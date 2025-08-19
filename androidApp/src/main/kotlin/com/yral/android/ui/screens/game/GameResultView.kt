package com.yral.android.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.game.GameResultConstant.RESULT_ANIMATION_DURATION
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
import kotlin.math.abs

private object GameResultConstant {
    const val RESULT_ANIMATION_DURATION = 400L
}

@Composable
fun GameResultView(
    modifier: Modifier,
    icon: GameIcon,
    coinDelta: Int,
    errorMessage: String = "",
    originalPos: Float,
) {
    val iconOffsetX = remember { Animatable(originalPos) }
    GameStripBackground(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp,
                Alignment.Start,
            ),
    ) {
        GameResultViewIcon(icon, originalPos, iconOffsetX)
        if (iconOffsetX.value == 0f) {
            Text(
                text = gameResultText(icon.imageName, coinDelta, errorMessage),
            )
        }
    }
}

@Composable
private fun GameResultViewIcon(
    icon: GameIcon,
    originalPos: Float,
    iconOffsetX: Animatable<Float, AnimationVector1D>,
) {
    LaunchedEffect(Unit) {
        iconOffsetX.snapTo(originalPos)
        iconOffsetX.animateTo(
            targetValue = 0f,
            animationSpec =
                tween(
                    durationMillis = RESULT_ANIMATION_DURATION.toInt(),
                    easing = FastOutLinearInEasing,
                ),
        )
    }
    GameIcon(
        modifier =
            Modifier
                .size(46.dp)
                .graphicsLayer { translationX = iconOffsetX.value },
        icon = icon,
    )
}

@Composable
private fun gameResultText(
    iconName: GameIconNames,
    coinDelta: Int,
    errorMessage: String = "",
): AnnotatedString =
    buildAnnotatedString {
        val textStyle = LocalAppTopography.current.mdBold
        val spanStyle =
            SpanStyle(
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily,
                fontWeight = textStyle.fontWeight,
                color = YralColors.Green50,
            )
        if (errorMessage.isNotEmpty()) {
            withStyle(spanStyle.plus(SpanStyle(color = YralColors.Red300))) {
                append(errorMessage)
            }
            return@buildAnnotatedString
        }
        if (coinDelta > 0) {
            withStyle(spanStyle) {
                append(
                    stringResource(
                        R.string.was_most_people_choice,
                        iconName.name.lowercase().capitalize(Locale.current),
                    ),
                )
                append(" ")
            }
            withStyle(spanStyle.plus(SpanStyle(color = YralColors.Green300))) {
                append(
                    stringResource(
                        R.string.you_win_x_coins,
                        coinDelta,
                    ),
                )
            }
        } else {
            withStyle(spanStyle) {
                append(
                    stringResource(R.string.not_most_popular_pick),
                )
                append(" ")
            }
            withStyle(spanStyle.plus(SpanStyle(color = YralColors.Red300))) {
                append(
                    stringResource(
                        R.string.you_lost_x_coins,
                        abs(coinDelta),
                    ),
                )
            }
        }
    }
