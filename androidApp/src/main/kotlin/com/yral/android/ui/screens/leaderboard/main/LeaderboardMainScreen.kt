package com.yral.android.ui.screens.leaderboard.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.leaderboard.LeaderboardRow
import com.yral.android.ui.screens.leaderboard.LeaderboardTableHeader
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.features.game.viewmodel.LeaderBoardViewModel
import org.koin.compose.viewmodel.koinViewModel

@Suppress("LongMethod", "UnusedParameter")
@Composable
fun LeaderboardMainScreen(
    component: LeaderboardMainComponent,
    modifier: Modifier = Modifier,
    viewModel: LeaderBoardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.leaderBoardTelemetry.leaderboardPageViewed()
        viewModel.loadData()
    }
    Box(modifier = modifier) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            // Trophies
            item {
                TrophyGallery(
                    isLoading = state.isLoading,
                    leaderboard = if (state.isLoading) emptyList() else state.leaderboard,
                    selectedMode = state.selectedMode,
                    selectMode = { viewModel.selectMode(it) },
                    countDownMs = state.countDownMs,
                    blinkCountDown = state.blinkCountDown,
                    openHistory = { component.openDailyHistory() },
                )
            }
            // Table Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                LeaderboardTableHeader()
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!state.isLoading && state.error == null) {
                // Show current user first if available
                state.currentUser?.let { user ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            LeaderboardRow(
                                position = user.leaderboardPosition,
                                userPrincipalId = user.userPrincipalId,
                                profileImageUrl = user.profileImageUrl,
                                wins = user.wins,
                                isCurrentUser = true,
                                decorateCurrentUser = true,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Leaderboard items
                items(state.leaderboard) { item ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        LeaderboardRow(
                            position = item.position,
                            userPrincipalId = item.userPrincipalId,
                            profileImageUrl = item.profileImage,
                            wins = item.wins,
                            isCurrentUser = viewModel.isCurrentUser(item.userPrincipalId),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(68.dp))
                }
            }
            if (!state.isLoading && state.error != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error ?: "",
                            style = LocalAppTopography.current.baseMedium,
                            color = YralColors.Neutral500,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                YralLoader()
            }
        }
    }
}

object LeaderboardMainScreenConstants {
    const val POSITION_TEXT_WEIGHT = 0.17f
    const val USER_DETAIL_WEIGHT = 0.55f
    const val COIN_BALANCE_WEIGHT = 0.28f
    const val MAX_CHAR_OF_NAME = 12
    const val COUNT_DOWN_BG_ALPHA = 0.8f
    const val COUNT_DOWN_ANIMATION_DURATION = 1000
}
