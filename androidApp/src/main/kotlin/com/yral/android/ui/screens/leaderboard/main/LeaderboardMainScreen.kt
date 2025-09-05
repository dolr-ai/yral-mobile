package com.yral.android.ui.screens.leaderboard.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.PROFILE_IMAGE_SIZE
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.getProfileImageRing
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.getTextDecoration
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.getUserBriefBorder
import com.yral.android.ui.screens.leaderboard.main.LeaderboardMainScreenConstants.COIN_BALANCE_WEIGHT
import com.yral.android.ui.screens.leaderboard.main.LeaderboardMainScreenConstants.MAX_CHAR_OF_NAME
import com.yral.android.ui.screens.leaderboard.main.LeaderboardMainScreenConstants.POSITION_TEXT_WEIGHT
import com.yral.android.ui.screens.leaderboard.main.LeaderboardMainScreenConstants.USER_DETAIL_WEIGHT
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.YralLoader
import com.yral.android.ui.widgets.YralMaskedVectorTextV2
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
    LaunchedEffect(Unit) { viewModel.leaderBoardTelemetry.leaderboardPageViewed() }
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
                            isCurrentUser = false,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(68.dp))
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

@Composable
private fun LeaderboardTableHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.position),
            modifier = Modifier.weight(POSITION_TEXT_WEIGHT),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.player_id),
            modifier =
                Modifier
                    .weight(USER_DETAIL_WEIGHT)
                    .padding(start = PROFILE_IMAGE_SIZE.dp + 8.dp),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.games_won),
            modifier = Modifier.weight(COIN_BALANCE_WEIGHT),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LeaderboardRow(
    position: Int,
    userPrincipalId: String,
    profileImageUrl: String,
    wins: Long,
    isCurrentUser: Boolean,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(42.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isCurrentUser) {
                        YralColors.Pink400
                    } else {
                        YralColors.Neutral900
                    },
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        UserBriefWithBorder(position, isCurrentUser) {
            UserBriefContent(
                position,
                userPrincipalId,
                profileImageUrl,
                wins,
                isCurrentUser,
            )
        }
    }
}

@Composable
private fun UserBriefWithBorder(
    position: Int,
    isCurrentUser: Boolean,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val border = getUserBriefBorder(position)
        if (border > 0 && !isCurrentUser) {
            Image(
                painter = painterResource(border),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
        content()
    }
}

@Composable
private fun UserBriefContent(
    position: Int,
    userPrincipalId: String,
    profileImageUrl: String,
    wins: Long,
    isCurrentUser: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
    ) {
        // Position column
        Row(
            modifier = Modifier.weight(POSITION_TEXT_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBriefPositionNumber(position, isCurrentUser)
        }
        // Player ID column with avatar
        Row(
            modifier = Modifier.weight(USER_DETAIL_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBriefProfileImage(position, profileImageUrl)
            Spacer(modifier = Modifier.width(8.dp))
            UserBriefProfileName(position, userPrincipalId, isCurrentUser)
        }
        // Games won
        Box(
            modifier = Modifier.weight(COIN_BALANCE_WEIGHT),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = wins.toString(),
                style = LocalAppTopography.current.baseBold,
                color = YralColors.Neutral50,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UserBriefPositionNumber(
    position: Int,
    isCurrentUser: Boolean,
) {
    val decoration = getTextDecoration(position)
    if (decoration != 0 && !isCurrentUser) {
        YralMaskedVectorTextV2(
            text = "#$position",
            vectorRes = decoration,
            textStyle = LocalAppTopography.current.baseBold,
            modifier = Modifier.width(21.dp),
            textOverflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    } else {
        Text(
            text = "#$position",
            style = LocalAppTopography.current.baseBold,
            color = YralColors.Neutral50,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun UserBriefProfileName(
    position: Int,
    name: String,
    isCurrentUser: Boolean,
) {
    if (isCurrentUser) {
        Text(
            text = stringResource(R.string.you),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.NeutralTextPrimary,
        )
    } else {
        UserBriefGradientProfileName(
            position = position,
            name =
                name
                    .take(MAX_CHAR_OF_NAME)
                    .plus("..."),
        )
    }
}

@Composable
private fun UserBriefGradientProfileName(
    position: Int,
    name: String,
) {
    val decoration = getTextDecoration(position)
    if (decoration != 0) {
        YralMaskedVectorTextV2(
            text = name,
            vectorRes = decoration,
            textStyle = LocalAppTopography.current.baseMedium,
            maxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            modifier = Modifier.wrapContentSize(),
        )
    } else {
        Text(
            text = name,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.NeutralTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun UserBriefProfileImage(
    position: Int,
    profileImageUrl: String,
    size: Dp = PROFILE_IMAGE_SIZE.dp,
) {
    Box(modifier = Modifier.wrapContentSize()) {
        YralAsyncImage(
            imageUrl = profileImageUrl,
            modifier = Modifier.size(size),
            backgroundColor = YralColors.ProfilePicBackground,
        )
        val profileImageRing = getProfileImageRing(position)
        if (profileImageRing > 0) {
            Image(
                painter = painterResource(profileImageRing),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
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
