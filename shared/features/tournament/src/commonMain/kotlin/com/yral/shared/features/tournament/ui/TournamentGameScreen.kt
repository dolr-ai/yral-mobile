@file:Suppress("MaxLineLength")

package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
import com.yral.shared.features.game.ui.SmileyGame
import com.yral.shared.features.tournament.viewmodel.TournamentGameState
import com.yral.shared.features.tournament.viewmodel.TournamentGameViewModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.tournament.generated.resources.exit
import yral_mobile.shared.features.tournament.generated.resources.ic_timer
import yral_mobile.shared.features.tournament.generated.resources.tournament_diamond
import yral_mobile.shared.features.tournament.generated.resources.tournament_ends_in
import yral_mobile.shared.features.tournament.generated.resources.tournament_ingame_rank
import yral_mobile.shared.features.tournament.generated.resources.tournament_most_people_chose
import yral_mobile.shared.features.tournament.generated.resources.tournament_not_most_popular_pick
import yral_mobile.shared.features.tournament.generated.resources.tournament_you_lost_diamonds
import yral_mobile.shared.features.tournament.generated.resources.tournament_you_win_diamonds
import yral_mobile.shared.features.tournament.generated.resources.trophy
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.exclamation
import yral_mobile.shared.libs.designsystem.generated.resources.ic_how_to_play
import yral_mobile.shared.libs.designsystem.generated.resources.shadow
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import yral_mobile.shared.features.tournament.generated.resources.Res as TournamentRes
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

/**
 * Top overlay for tournament game screen showing header, leaderboard, and diamonds.
 */
@Composable
fun TournamentTopOverlay(
    gameState: TournamentGameState,
    tournamentTitle: String,
    onLeaderboardClick: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(DesignRes.drawable.shadow),
                    contentScale = ContentScale.FillBounds,
                ).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TournamentHeader(
            tournamentTitle = tournamentTitle,
            diamonds = gameState.diamonds,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopStart),
        )
        TournamentLeaderboardBadge(
            position = gameState.position,
            activeParticipantCount = gameState.activeParticipantCount,
            onClick = onLeaderboardClick,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 72.dp),
        )
    }
}

@Composable
private fun TournamentHeader(
    tournamentTitle: String,
    diamonds: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "back",
            colorFilter = ColorFilter.tint(Color.White),
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable { onBack() },
        )
        Spacer(modifier = Modifier.width(8.dp))
        TournamentTitlePill(
            title = tournamentTitle,
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        TournamentDiamondPill(diamonds = diamonds)
    }
}

@Suppress("MagicNumber")
@Composable
private fun TournamentTitlePill(title: String) {
    Box(contentAlignment = Alignment.CenterStart) {
        Text(
            modifier =
                Modifier
                    .padding(start = 14.dp)
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(YralColors.Yellow400, Color.Transparent),
                            ),
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
            text = title.ifBlank { "Tournament" },
            style = LocalAppTopography.current.mdBold,
            color = Color(0xFFFFF9EB),
            maxLines = 1,
        )
        Image(
            painter = painterResource(TournamentRes.drawable.trophy),
            contentDescription = null,
            modifier =
                Modifier
                    .width(28.dp)
                    .height(33.dp),
        )
    }
}

@Composable
private fun TournamentDiamondPill(diamonds: Int) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(YralColors.Yellow400)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = diamonds.toString(),
            style = LocalAppTopography.current.mdSemiBold,
            color = YralColors.Neutral50,
        )
        Image(
            painter = painterResource(TournamentRes.drawable.tournament_diamond),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TournamentLeaderboardBadge(
    position: Int,
    activeParticipantCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rankText =
        if (position > 0) {
            if (activeParticipantCount > 0) {
                "#$position / $activeParticipantCount"
            } else {
                "#$position"
            }
        } else {
            "--"
        }
    Box(
        modifier = modifier.clickable { onClick() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Image(
            painter = painterResource(TournamentRes.drawable.tournament_ingame_rank),
            contentDescription = null,
            modifier = Modifier.size(50.dp),
        )
        Box(
            modifier =
                Modifier
                    .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                    .background(YralColors.Red300, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = rankText,
                style = LocalAppTopography.current.baseBold,
                color = Color.White,
            )
        }
    }
}

/**
 * Bottom overlay for tournament game screen showing game icons and vote results.
 */
@Suppress("LongMethod")
@Composable
fun TournamentBottomOverlay(
    pageNo: Int,
    feedDetails: FeedDetails,
    gameState: TournamentGameState,
    gameViewModel: TournamentGameViewModel,
    timeLeftMs: Long,
    isHotOrNot: Boolean,
    onHowToPlayClick: () -> Unit,
) {
    gameViewModel.hasVotedOnVideo(feedDetails.videoID)
    val voteResult = gameViewModel.getVoteResult(feedDetails.videoID)
    // Use produceState to observe only THIS video's emojis, avoiding recomposition
    // when other videos' emojis change. Start with empty list to hide static emojis
    // until dynamic ones are loaded.
    val videoIcons by produceState(
        initialValue = emptyList(),
        key1 = feedDetails.videoID,
    ) {
        gameViewModel.videoEmojisState
            .map { it[feedDetails.videoID] ?: emptyList() }
            .distinctUntilChanged()
            .collect { value = it }
    }
    // Construct selectedIcon from vote result data directly instead of looking up by ID
    // This ensures we have the correct icon even if videoIcons was updated after voting
    val selectedIcon =
        voteResult?.smiley?.let { smiley ->
            GameIcon(
                id = smiley.id,
                imageName = GameIconNames.UNKNOWN,
                imageUrl = smiley.imageUrl ?: "",
                clickAnimation = smiley.clickAnimation ?: "",
                unicode = smiley.unicode ?: "",
            )
        }
    val diamondDelta = voteResult?.diamondDelta ?: 0
    val overlayBottomPadding = 120.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Only show smiley game for smiley tournaments, not Hot or Not
        if (videoIcons.isNotEmpty() && !isHotOrNot) {
            SmileyGame(
                gameIcons = videoIcons,
                clickedIcon = selectedIcon,
                isLoading = gameState.isLoading,
                coinDelta = diamondDelta,
                errorMessage = "",
                resultContent = { icon, coinDelta, errorMessage ->
                    TournamentGameResultContent(
                        iconName = icon.imageName,
                        coinDelta = coinDelta,
                        errorMessage = errorMessage,
                    )
                },
                onIconClicked = { icon, _ ->
                    gameViewModel.setClickedIcon(icon, feedDetails)
                },
                hasShownCoinDeltaAnimation =
                    gameViewModel.hasShownCoinDeltaAnimation(feedDetails.videoID),
                onDeltaAnimationComplete = {
                    gameViewModel.markCoinDeltaAnimationShown(feedDetails.videoID)
                },
                nudgeType = null,
                pageNo = pageNo,
                onNudgeAnimationComplete = {},
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
        Image(
            painter = painterResource(DesignRes.drawable.ic_how_to_play),
            contentDescription = "how to play",
            contentScale = ContentScale.None,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = overlayBottomPadding + 8.dp)
                    .size(32.dp)
                    .clickable { onHowToPlayClick() },
        )
        TournamentTimerPill(
            timeLeftMs = timeLeftMs,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = overlayBottomPadding + 12.dp),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun TournamentGameResultContent(
    iconName: GameIconNames,
    coinDelta: Int,
    errorMessage: String,
) {
    val textStyle = LocalAppTopography.current.mdBold
    val baseStyle =
        SpanStyle(
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight,
            color = YralColors.Green50,
        )
    if (errorMessage.isNotEmpty()) {
        Text(
            text = errorMessage,
            style = textStyle,
            color = YralColors.Red300,
        )
        return
    }
    val density = LocalDensity.current
    val placeholderSize = with(density) { 16.dp.toSp() }
    val inlineContent =
        mapOf(
            "diamond" to
                InlineTextContent(
                    Placeholder(
                        width = placeholderSize,
                        height = placeholderSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    Image(
                        painter = painterResource(TournamentRes.drawable.tournament_diamond),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
        )
    val resultText =
        buildAnnotatedString {
            if (coinDelta > 0) {
                withStyle(baseStyle) {
                    append(
                        stringResource(
                            TournamentRes.string.tournament_most_people_chose,
                            iconName.name.lowercase().capitalize(Locale.current),
                        ),
                    )
                    append(" ")
                }
                withStyle(baseStyle.plus(SpanStyle(color = YralColors.Green300))) {
                    append(
                        stringResource(
                            TournamentRes.string.tournament_you_win_diamonds,
                            abs(coinDelta),
                        ),
                    )
                    append(" ")
                }
            } else {
                withStyle(baseStyle) {
                    append(
                        stringResource(
                            TournamentRes.string.tournament_not_most_popular_pick,
                        ),
                    )
                    append(" ")
                }
                withStyle(baseStyle.plus(SpanStyle(color = YralColors.Red300))) {
                    append(
                        stringResource(
                            TournamentRes.string.tournament_you_lost_diamonds,
                            abs(coinDelta),
                        ),
                    )
                    append(" ")
                }
            }
            appendInlineContent("diamond", "[diamond]")
        }
    Text(text = resultText, inlineContent = inlineContent)
}

@Suppress("MagicNumber")
@Composable
private fun TournamentTimerPill(
    timeLeftMs: Long,
    modifier: Modifier = Modifier,
) {
    val timeLeftColor =
        when {
            timeLeftMs <= 60_000L -> Color(0xFFFF6353)
            timeLeftMs <= 300_000L -> YralColors.Yellow200
            else -> YralColors.Neutral300
        }
    val remainingDuration = formatRemainingDuration(timeLeftMs.milliseconds)
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x660A0A0A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(TournamentRes.drawable.ic_timer),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text =
                buildAnnotatedString {
                    append(
                        stringResource(
                            TournamentRes.string.tournament_ends_in,
                            "",
                        ).trim(),
                    )
                    append(" ")
                    withStyle(SpanStyle(color = timeLeftColor)) {
                        append(remainingDuration)
                    }
                },
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral300,
            maxLines = 1,
        )
    }
}

@Composable
fun ColumnScope.TournamentGameActionsRight(
    onExit: () -> Unit,
    onReport: () -> Unit,
) {
    Image(
        painter = painterResource(TournamentRes.drawable.exit),
        contentDescription = "exit tournament",
        colorFilter = ColorFilter.tint(Color.White),
        modifier =
            Modifier
                .size(36.dp)
                .clickable { onExit() },
    )
    Image(
        painter = painterResource(DesignRes.drawable.exclamation),
        contentDescription = "report video",
        colorFilter = ColorFilter.tint(Color.White),
        modifier =
            Modifier
                .size(36.dp)
                .clickable { onReport() },
    )
    Spacer(modifier = Modifier.height(0.dp))
}

/**
 * Dialog shown when tournament has ended.
 */
@Suppress("LongMethod")
@Composable
fun TournamentEndedDialog(
    onViewLeaderboard: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "\uD83C\uDFC6", // Trophy emoji
                fontSize = 48.sp,
            )
            Text(
                text = "Tournament Ended!",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "The tournament has ended. Check the leaderboard to see your final ranking!",
                color = Color.Gray,
                fontSize = 14.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .clickable { onExit() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Exit",
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(YralColors.Pink300)
                            .clickable { onViewLeaderboard() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "View Leaderboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
