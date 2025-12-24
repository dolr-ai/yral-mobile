@file:Suppress("MaxLineLength")

package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes
import yral_mobile.shared.libs.designsystem.generated.resources.shadow

/**
 * Top overlay for tournament game screen showing rank, timer, and diamonds.
 */
@Composable
fun TournamentTopOverlay(
    gameState: TournamentGameState,
    onLeaderboardClick: () -> Unit,
    onTimeUp: () -> Unit,
) {
    var timeLeftMs by remember(gameState.endEpochMs) {
        mutableLongStateOf(
            maxOf(0L, gameState.endEpochMs - System.currentTimeMillis()),
        )
    }

    LaunchedEffect(gameState.endEpochMs) {
        while (timeLeftMs > 0) {
            delay(1000L)
            timeLeftMs = maxOf(0L, gameState.endEpochMs - System.currentTimeMillis())
        }
        if (timeLeftMs <= 0 && gameState.endEpochMs > 0) {
            onTimeUp()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .paint(
                painter = painterResource(DesignRes.drawable.shadow),
                contentScale = ContentScale.FillBounds,
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Leaderboard / Rank
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onLeaderboardClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "#${gameState.position}",
                    color = Color(0xFFFFD700), // Gold color
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Timer
            TimeRemaining(timeLeftMs = timeLeftMs)

            // Diamonds
            DiamondBalance(diamonds = gameState.diamonds)
        }
    }
}

@Composable
private fun TimeRemaining(timeLeftMs: Long) {
    val hours = (timeLeftMs / 3600000).toInt()
    val minutes = ((timeLeftMs % 3600000) / 60000).toInt()
    val seconds = ((timeLeftMs % 60000) / 1000).toInt()

    val timeText = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = timeText,
            color = if (timeLeftMs < 60000) Color.Red else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DiamondBalance(diamonds: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF42A5F5),
                    ),
                ),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "\uD83D\uDC8E", // Diamond emoji
            fontSize = 14.sp,
        )
        Text(
            text = diamonds.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Bottom overlay for tournament game screen showing game icons and vote results.
 */
@Composable
fun TournamentBottomOverlay(
    feedDetails: FeedDetails,
    gameState: TournamentGameState,
    gameViewModel: TournamentGameViewModel,
    scrollToNext: () -> Unit,
) {
    val hasVoted = gameViewModel.hasVotedOnVideo(feedDetails.videoID)
    val voteResult = gameViewModel.getVoteResult(feedDetails.videoID)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
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
                    isLoading = gameState.isLoading,
                    hasVoted = hasVoted,
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
private fun TournamentGameIcons(
    gameIcons: List<GameIcon>,
    isLoading: Boolean,
    hasVoted: Boolean,
    onIconClick: (GameIcon) -> Unit,
) {
    GameIconStrip(
        modifier = Modifier,
        gameIcons = gameIcons,
        clickedIcon = null,
        onIconClicked = { icon -> onIconClick(icon) },
        isLoading = isLoading || hasVoted,
        coinDelta = 0,
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
