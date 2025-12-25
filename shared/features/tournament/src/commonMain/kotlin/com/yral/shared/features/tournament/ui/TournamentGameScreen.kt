@file:Suppress("MaxLineLength")

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.ui.GameIconStrip
import com.yral.shared.features.tournament.domain.model.VoteOutcome
import com.yral.shared.features.tournament.viewmodel.TournamentGameState
import com.yral.shared.features.tournament.viewmodel.TournamentGameViewModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.features.tournament.generated.resources.ic_timer
import yral_mobile.shared.features.tournament.generated.resources.tournament_diamond
import yral_mobile.shared.features.tournament.generated.resources.tournament_exit
import yral_mobile.shared.features.tournament.generated.resources.tournament_leaderboard
import yral_mobile.shared.features.tournament.generated.resources.trophy
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.exclamation
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes
import yral_mobile.shared.libs.designsystem.generated.resources.shadow
import yral_mobile.shared.libs.designsystem.generated.resources.shadow_bottom
import yral_mobile.shared.libs.designsystem.generated.resources.victory_cup
import kotlin.time.Duration.Companion.milliseconds
import yral_mobile.shared.features.tournament.generated.resources.Res as TournamentRes

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
        modifier = Modifier
            .fillMaxWidth()
            .paint(
                painter = painterResource(DesignRes.drawable.shadow),
                contentScale = ContentScale.FillBounds,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TournamentHeader(
            tournamentTitle = tournamentTitle,
            diamonds = gameState.diamonds,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopStart),
        )
        TournamentLeaderboardBadge(
            position = gameState.position,
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

@Composable
private fun TournamentTitlePill(
    title: String,
) {
    Box(contentAlignment = Alignment.CenterStart) {
        Text(
            modifier = Modifier
                .padding(start = 14.dp)
                .background(
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(YralColors.Yellow400, Color.Transparent),
                        ),
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            text = title.ifBlank { "Tournament" },
            style = LocalAppTopography.current.mdBold,
            color = Color(0xFFFFF9EB),
            maxLines = 1,
        )
        Image(
            painter = painterResource(TournamentRes.drawable.trophy),
            contentDescription = null,
            modifier = Modifier
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rankText = if (position > 0) "#$position" else "--"
    Box(
        modifier = modifier.clickable { onClick() },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Image(
            painter = painterResource(TournamentRes.drawable.tournament_leaderboard),
            contentDescription = null,
            modifier = Modifier.size(50.dp),
        )
        Box(
            modifier =
                Modifier
                    .padding(end = 2.dp, bottom = 2.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                    .background(Color(0xFFF14331), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
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
@Composable
fun TournamentBottomOverlay(
    feedDetails: FeedDetails,
    pageNo: Int,
    gameState: TournamentGameState,
    gameViewModel: TournamentGameViewModel,
    timeLeftMs: Long,
    onExit: () -> Unit,
    onReport: () -> Unit,
    scrollToNext: () -> Unit,
) {
    val hasVoted = gameViewModel.hasVotedOnVideo(feedDetails.videoID)
    val voteResult = gameViewModel.getVoteResult(feedDetails.videoID)
    val selectedIcon =
        voteResult?.smiley?.id?.let { voteId ->
            gameState.gameIcons.firstOrNull { it.id == voteId }
        }
    val diamondDelta =
        when (voteResult?.outcome) {
            VoteOutcome.WIN -> 1
            VoteOutcome.LOSS -> -1
            null -> 0
        }
    val overlayBottomPadding = 120.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(260.dp)
                    .paint(
                        painter = painterResource(DesignRes.drawable.shadow_bottom),
                        contentScale = ContentScale.FillBounds,
                    ),
        )
        TournamentTimerPill(
            timeLeftMs = timeLeftMs,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = overlayBottomPadding + 12.dp),
        )
        ActionsRight(
            onExit = onExit,
            onReport = onReport,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = overlayBottomPadding + 4.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Show result indicator after voting
            if (hasVoted && voteResult != null) {
                VoteResultIndicator(
                    outcome = voteResult.outcome,
                    diamondDelta = if (voteResult.outcome == VoteOutcome.WIN) 1 else -1,
                )
            }

            // Game icons strip
            if (gameState.gameIcons.isNotEmpty()) {
                TournamentGameIcons(
                    gameIcons = gameState.gameIcons,
                    selectedIcon = selectedIcon,
                    isLoading = gameState.isLoading,
                    hasVoted = hasVoted,
                    diamondDelta = diamondDelta,
                    onIconClick = { icon ->
                        gameViewModel.setClickedIcon(icon, feedDetails)
                    },
                )
            }
        }
    }

    // Auto-scroll after vote
    LaunchedEffect(hasVoted) {
        if (hasVoted) {
            delay(1500L)
            scrollToNext()
        }
    }
}

@Composable
private fun VoteResultIndicator(
    outcome: VoteOutcome,
    diamondDelta: Int,
) {
    val (color, text) = when (outcome) {
        VoteOutcome.WIN -> Color(0xFF4CAF50) to "+$diamondDelta \uD83D\uDC8E"
        VoteOutcome.LOSS -> Color(0xFFF44336) to "$diamondDelta \uD83D\uDC8E"
    }

    Box(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.9f))
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TournamentTimerPill(
    timeLeftMs: Long,
    modifier: Modifier = Modifier,
) {
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
            text = "Tournament ends in ${formatRemainingDuration(timeLeftMs.milliseconds)}",
            style = LocalAppTopography.current.regMedium,
            color = Color(0xFFD4D4D4),
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionsRight(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
    onReport: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(TournamentRes.drawable.tournament_exit),
            contentDescription = "exit tournament",
            modifier =
                Modifier
                    .size(36.dp)
                    .clickable { onExit() },
        )
        Image(
            painter = painterResource(DesignRes.drawable.exclamation),
            contentDescription = "report video",
            modifier =
                Modifier
                    .size(36.dp)
                    .clickable { onReport() },
        )
    }
}

@Composable
private fun TournamentGameIcons(
    gameIcons: List<GameIcon>,
    selectedIcon: GameIcon?,
    isLoading: Boolean,
    hasVoted: Boolean,
    diamondDelta: Int,
    onIconClick: (GameIcon) -> Unit,
) {
    GameIconStrip(
        modifier = Modifier,
        gameIcons = gameIcons,
        clickedIcon = selectedIcon,
        onIconClicked = { icon -> onIconClick(icon) },
        isLoading = isLoading || hasVoted,
        coinDelta = diamondDelta,
    )
}

/**
 * Dialog shown when tournament has ended.
 */
@Composable
fun TournamentEndedDialog(
    onViewLeaderboard: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
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
                    modifier = Modifier
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
                    modifier = Modifier
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

/**
 * Dialog shown when user runs out of diamonds.
 */
@Composable
fun NoDiamondsDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "\uD83D\uDC8E",
                fontSize = 48.sp,
            )
            Text(
                text = "No Diamonds Left!",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "You've used all your diamonds. Wait for the tournament to end to see your final ranking.",
                color = Color.Gray,
                fontSize = 14.sp,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(YralColors.Pink300)
                    .clickable { onExit() }
                    .padding(horizontal = 32.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Exit Tournament",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
