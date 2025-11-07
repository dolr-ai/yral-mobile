package com.yral.shared.features.game.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.ui.SmileyGameConstants.MANDATORY_NUDGE_ANIMATION_ICON_ITERATIONS
import com.yral.shared.features.game.ui.SmileyGameConstants.NUDGE_ANIMATION_DURATION
import com.yral.shared.features.game.ui.SmileyGameConstants.NUDGE_ANIMATION_ICON_ITERATIONS
import com.yral.shared.features.game.viewmodel.GameViewModel
import com.yral.shared.features.game.viewmodel.NudgeType
import com.yral.shared.libs.designsystem.component.YralFeedback
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.game.generated.resources.Res
import yral_mobile.shared.features.game.generated.resources.smiley_game_nudge_1
import yral_mobile.shared.features.game.generated.resources.smiley_game_nudge_2
import yral_mobile.shared.features.game.generated.resources.smiley_game_nudge_arrow
import yral_mobile.shared.features.game.generated.resources.smiley_game_nudge_mandatory
import yral_mobile.shared.features.game.generated.resources.smiley_game_nudge_stars
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun Game(
    feedDetails: FeedDetails,
    pageNo: Int,
    gameViewModel: GameViewModel,
) {
    val gameState by gameViewModel.state.collectAsStateWithLifecycle()
    if (gameState.gameIcons.isNotEmpty()) {
        SmileyGame(
            gameIcons = gameState.gameIcons,
            clickedIcon = gameState.gameResult[feedDetails.videoID]?.first,
            onIconClicked = { icon, isTutorialVote ->
                gameViewModel.setClickedIcon(
                    icon = icon,
                    feedDetails = feedDetails,
                    isTutorialVote = isTutorialVote,
                )
            },
            coinDelta = gameViewModel.getFeedGameResult(feedDetails.videoID),
            errorMessage = gameViewModel.getFeedGameResultError(feedDetails.videoID),
            isLoading = gameState.isLoading,
            hasShownCoinDeltaAnimation =
                gameViewModel.hasShownCoinDeltaAnimation(
                    videoId = feedDetails.videoID,
                ),
            onDeltaAnimationComplete = {
                gameViewModel.markCoinDeltaAnimationShown(
                    videoId = feedDetails.videoID,
                )
            },
            nudgeType = gameState.nudgeType,
            pageNo = pageNo,
            onNudgeAnimationComplete = {
                gameViewModel.setSmileyGameNudgeShown(feedDetails)
            },
        )
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun SmileyGame(
    gameIcons: List<GameIcon>,
    clickedIcon: GameIcon?,
    isLoading: Boolean,
    coinDelta: Int = 0,
    errorMessage: String = "",
    onIconClicked: (emoji: GameIcon, isTutorialVote: Boolean) -> Unit,
    hasShownCoinDeltaAnimation: Boolean,
    onDeltaAnimationComplete: () -> Unit,
    nudgeType: NudgeType?,
    pageNo: Int,
    onNudgeAnimationComplete: () -> Unit,
) {
    var animateBubbles by remember { mutableStateOf(false) }
    var iconPositions by remember { mutableStateOf(mapOf<Int, Float>()) }
    val bubbleAnimationComplete =
        !animateBubbles ||
            (clickedIcon != null && clickedIcon.getBubbleResource() == null && clickedIcon.clickAnimation.isEmpty())
    val resultViewVisible = (coinDelta != 0 || errorMessage.isNotEmpty()) && bubbleAnimationComplete
    var animatingNudgeIconPosition by remember { mutableStateOf<Int?>(null) }
    var nudgeIterationCount by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        when {
            resultViewVisible -> {
                SmileyGameResult(
                    clickedIcon = clickedIcon,
                    coinDelta = coinDelta,
                    errorMessage = errorMessage,
                    originalPos = iconPositions[gameIcons.indexOf(clickedIcon)] ?: 0f,
                    hasShownCoinDeltaAnimation = hasShownCoinDeltaAnimation,
                    onAnimationComplete = onDeltaAnimationComplete,
                )
            }
            else -> {
                SmileyGameNudge(
                    modifier = Modifier.align(Alignment.BottomCenter).clickable { onNudgeAnimationComplete() },
                    pageNo = pageNo,
                    nudgeType = nudgeType,
                    animatingNudgeIconPosition = animatingNudgeIconPosition,
                    startNudgeAnimation = {
                        animatingNudgeIconPosition = 0
                        nudgeIterationCount = 0
                    },
                    dismissNudgeAnimation = {
                        animatingNudgeIconPosition = null
                        nudgeIterationCount = 0
                    },
                )
                GameIconStrip(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    gameIcons = gameIcons,
                    clickedIcon = clickedIcon,
                    onIconClicked = {
                        animateBubbles = true
                        onIconClicked(it, nudgeType == NudgeType.INTRO && animatingNudgeIconPosition != null)
                    },
                    isLoading = isLoading,
                    coinDelta = coinDelta,
                    onIconPositioned = { id, xPos ->
                        iconPositions = iconPositions.plus(id to xPos)
                    },
                    animatingNudgeIconPosition = animatingNudgeIconPosition,
                    onIconAnimationComplete = {
                        animatingNudgeIconPosition =
                            animatingNudgeIconPosition?.let { currentIndex ->
                                when {
                                    currentIndex + 1 < gameIcons.size -> currentIndex + 1
                                    !isNudgeIterationValid(
                                        nudgeIteration = ++nudgeIterationCount,
                                        isMandatory = nudgeType == NudgeType.MANDATORY,
                                    ) ->
                                        run {
                                            nudgeIterationCount = 0
                                            onNudgeAnimationComplete()
                                            null
                                        }
                                    else -> 0
                                }
                            }
                    },
                    setNudgeShown = {
                        animatingNudgeIconPosition = null
                        nudgeIterationCount = 0
                        onNudgeAnimationComplete()
                    },
                )
            }
        }
        if (animateBubbles) {
            clickedIcon?.let {
                GameIconBubbles(clickedIcon) {
                    animateBubbles = false
                }
            }
        }
    }
    if (resultViewVisible && !hasShownCoinDeltaAnimation) {
        YralFeedback(
            soundUri = if (coinDelta > 0) spilledCoinSoundUri() else coinLossSoundUri(),
            withHapticFeedback = true,
            hapticFeedbackType = HapticFeedbackType.LongPress,
        )
    }
}

fun coinLossSoundUri(): String = Res.getUri("files/audio/coin_loss.mp3")
fun spilledCoinSoundUri(): String = Res.getUri("files/audio/spilled_coin.mp3")

private fun isNudgeIterationValid(
    nudgeIteration: Int,
    isMandatory: Boolean,
) = when (isMandatory) {
    false -> nudgeIteration < NUDGE_ANIMATION_ICON_ITERATIONS
    true -> nudgeIteration < MANDATORY_NUDGE_ANIMATION_ICON_ITERATIONS
}

@Composable
private fun BoxScope.SmileyGameResult(
    clickedIcon: GameIcon?,
    coinDelta: Int,
    errorMessage: String,
    originalPos: Float,
    hasShownCoinDeltaAnimation: Boolean,
    onAnimationComplete: () -> Unit,
) {
    clickedIcon?.let {
        GameResultView(
            modifier = Modifier.align(Alignment.BottomCenter),
            icon = it,
            coinDelta = coinDelta,
            errorMessage = errorMessage,
            originalPos = originalPos,
        )
    }
    if (!hasShownCoinDeltaAnimation && errorMessage.isEmpty()) {
        CoinDeltaAnimation(
            text = coinDelta.toSignedString(),
            textColor =
                if (coinDelta > 0) {
                    YralColors.Green300.copy(alpha = 0.3f)
                } else {
                    YralColors.Red300.copy(alpha = 0.3f)
                },
            onAnimationEnd = onAnimationComplete,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SmileyGameNudge(
    modifier: Modifier = Modifier,
    pageNo: Int,
    nudgeType: NudgeType?,
    animatingNudgeIconPosition: Int?,
    startNudgeAnimation: () -> Unit,
    dismissNudgeAnimation: () -> Unit,
) {
    LaunchedEffect(nudgeType) {
        try {
            if (nudgeType != null) {
                startNudgeAnimation()
            } else {
                dismissNudgeAnimation()
            }
        } catch (e: CancellationException) {
            // Reset state safely on coroutine cancellation
            dismissNudgeAnimation()
            throw e
        } catch (_: Exception) {
            dismissNudgeAnimation()
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "nudge $pageNo")
    val tweenSpec =
        tween<Float>(
            durationMillis = NUDGE_ANIMATION_DURATION.toInt(),
            easing = FastOutLinearInEasing,
        )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
    )
    animatingNudgeIconPosition?.let {
        SmileyGameNudgeContent(
            modifier = modifier,
            alpha = alpha,
            offsetY = offsetY,
            isNudgeMandatory = nudgeType == NudgeType.MANDATORY,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SmileyGameNudgeContent(
    modifier: Modifier,
    alpha: Float,
    offsetY: Float,
    isNudgeMandatory: Boolean,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(YralColors.ScrimColorLight)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val density = LocalDensity.current
        var textWidth by remember { mutableIntStateOf(0) }
        if (!isNudgeMandatory) {
            Image(
                painter = painterResource(Res.drawable.smiley_game_nudge_stars),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(bottom = 226.dp)
                        .width(with(density) { textWidth.toDp() + 16.dp })
                        .height(130.dp)
                        .alpha(alpha),
                contentScale = ContentScale.FillBounds,
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp, start = 36.dp, end = 36.dp)
                    .offset(y = offsetY.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val textStyle = LocalAppTopography.current.xlBold
            val spanStyle =
                SpanStyle(
                    fontSize = textStyle.fontSize,
                    fontFamily = textStyle.fontFamily,
                    fontWeight = textStyle.fontWeight,
                    color = YralColors.Neutral50,
                )
            Text(
                text =
                    if (isNudgeMandatory) {
                        buildAnnotatedString {
                            withStyle(spanStyle) { append(stringResource(Res.string.smiley_game_nudge_mandatory)) }
                        }
                    } else {
                        buildAnnotatedString {
                            withStyle(spanStyle) { append(stringResource(Res.string.smiley_game_nudge_1)) }
                            withStyle(spanStyle) { append("\n") }
                            withStyle(style = spanStyle.copy(color = YralColors.Yellow200)) {
                                append(stringResource(Res.string.smiley_game_nudge_2))
                            }
                        }
                    },
                textAlign = TextAlign.Center,
                modifier = Modifier.onGloballyPositioned { textWidth = it.size.width },
            )
            Image(
                painter = painterResource(Res.drawable.smiley_game_nudge_arrow),
                contentDescription = "arrow",
                modifier = Modifier,
            )
        }
    }
}

private fun Int.toSignedString(): String =
    if (this >= 0) {
        "+$this"
    } else {
        "$this"
    }

object SmileyGameConstants {
    const val NUDGE_ANIMATION_DURATION = 600L
    const val NUDGE_ANIMATION_ICON_ITERATIONS = 3
    const val MANDATORY_NUDGE_ANIMATION_ICON_ITERATIONS = 2
}
