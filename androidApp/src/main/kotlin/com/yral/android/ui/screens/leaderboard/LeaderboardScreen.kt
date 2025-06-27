package com.yral.android.ui.screens.leaderboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.MAX_USERS_PRINCIPAL_LENGTH
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.MAX_USERS_WITH_DUPLICATE_RANK
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.PROFILE_IMAGE_SIZE
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.getProfileImageRing
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.getTextDecoration
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.getTrophyImageHeight
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.getTrophyImageOffset
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.getTrophyImageWidth
import com.yral.android.ui.screens.leaderboard.LeaderboardHelpers.getUserBriefBorder
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.COIN_BALANCE_WEIGHT
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.POSITION_TEXT_WEIGHT
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.USER_DETAIL_WEIGHT
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.YralLoader
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.android.ui.widgets.YralMaskedVectorTextV2
import com.yral.shared.features.game.domain.models.LeaderboardItem
import com.yral.shared.features.game.viewmodel.LeaderBoardViewModel

@Suppress("LongMethod")
@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    viewModel: LeaderBoardViewModel,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.leaderboard, state.currentUser) {
        val userWithSameBalance =
            state
                .leaderboard
                .filter { it.coins == state.currentUser?.coins }
        if (userWithSameBalance.isNotEmpty()) {
            viewModel.updateCurrentUserRank(userWithSameBalance[0].rank)
        }
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
                TrophyGallery(state.leaderboard)
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
                                rank = user.rank,
                                userPrincipalId = user.userPrincipalId,
                                profileImageUrl = user.profileImageUrl,
                                coins = user.coins,
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
                            rank = item.rank,
                            userPrincipalId = item.userPrincipalId,
                            profileImageUrl = item.profileImage,
                            coins = item.coins,
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
            text = stringResource(R.string.total_sats),
            modifier = Modifier.weight(COIN_BALANCE_WEIGHT),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun TrophyGallery(leaderboard: List<LeaderboardItem>) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Yellow400),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (leaderboard.isNotEmpty()) {
            YralLottieAnimation(
                modifier = Modifier.matchParentSize(),
                rawRes = R.raw.leaderboard_star,
                contentScale = ContentScale.FillBounds,
            )
        }
        // Header
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.leaderboard),
                style = LocalAppTopography.current.xlBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            if (leaderboard.size > 3) {
                Spacer(Modifier.height(28.dp))
                TrophyImages(leaderboard)
                Spacer(Modifier.height(12.dp))
                TrophyDetails(leaderboard)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ColumnScope.TrophyImages(leaderboard: List<LeaderboardItem>) {
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(42.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Trophy(
            rank = 1,
            profileImageUrl = getProfileImageForTrophy(1, leaderboard),
            trophyResource = R.drawable.silver_trophy,
        )
        Trophy(
            rank = 0,
            profileImageUrl = getProfileImageForTrophy(0, leaderboard),
            trophyResource = R.drawable.golden_trophy,
        )
        Trophy(
            rank = 2,
            profileImageUrl = getProfileImageForTrophy(2, leaderboard),
            trophyResource = R.drawable.bronze_trophy,
        )
    }
}

private fun getProfileImageForTrophy(
    rank: Int,
    leaderboard: List<LeaderboardItem>,
): String {
    val users = leaderboard.filter { it.rank == rank }
    return if (users.size == 1) {
        // users[0].profileImage
        ""
    } else {
        ""
    }
}

@Composable
private fun ColumnScope.TrophyDetails(leaderboard: List<LeaderboardItem>) {
    Column(modifier = Modifier.align(Alignment.CenterHorizontally)) {
        val rank0 by remember(leaderboard) { mutableStateOf(getTrophyDetailItem(0, leaderboard)) }
        val rank1 by remember(leaderboard) { mutableStateOf(getTrophyDetailItem(1, leaderboard)) }
        val rank2 by remember(leaderboard) { mutableStateOf(getTrophyDetailItem(2, leaderboard)) }
        val lines =
            if (setOf(rank0.first, rank1.first, rank2.first).any { it.contains(",") }) {
                2
            } else {
                1
            }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            TrophyDetailsItem(
                userPrincipalId = rank1.first,
                coins = rank1.second,
                lines = lines,
            )
            TrophyDetailsItem(
                userPrincipalId = rank0.first,
                coins = rank0.second,
                lines = lines,
            )
            TrophyDetailsItem(
                userPrincipalId = rank2.first,
                coins = rank2.second,
                lines = lines,
            )
        }
    }
}

private fun getTrophyDetailItem(
    rank: Int,
    leaderboard: List<LeaderboardItem>,
): Pair<String, Long> {
    val users = leaderboard.filter { it.rank == rank }
    return if (users.isNotEmpty()) {
        getTrophyDetailsUserTexts(users) to users[0].coins
    } else {
        "" to -1
    }
}

private fun getTrophyDetailsUserTexts(user: List<LeaderboardItem>): String =
    when (user.size) {
        1 -> user[0].userPrincipalId
        else ->
            user
                .take(MAX_USERS_WITH_DUPLICATE_RANK)
                .joinToString(", ") {
                    it.userPrincipalId.take(MAX_USERS_PRINCIPAL_LENGTH) + "..."
                }
    }

@Composable
private fun Trophy(
    rank: Int,
    profileImageUrl: String,
    trophyResource: Int,
) {
    val width = getTrophyImageWidth(rank)
    val height = getTrophyImageHeight(rank)
    val offset =
        getTrophyImageOffset(
            rank = rank,
            isProfileImageVisible = profileImageUrl.isNotEmpty(),
        )
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height + offset),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(id = trophyResource),
            contentDescription = "image description",
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .width(width)
                    .height(height)
                    .offset { IntOffset(0, offset.roundToPx()) },
        )
        if (profileImageUrl.isNotEmpty()) {
            UserBriefProfileImage(
                rank = rank,
                profileImageUrl = profileImageUrl,
                size = width,
            )
        }
    }
}

@Composable
private fun TrophyDetailsItem(
    userPrincipalId: String,
    lines: Int,
    coins: Long,
) {
    Column(
        modifier = Modifier.width(93.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = userPrincipalId,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.NeutralTextSecondary,
            minLines = lines,
            maxLines = lines,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        if (coins >= 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.satoshi),
                    contentDescription = "image description",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(23.dp),
                )
                Text(
                    text = coins.toString(),
                    style = LocalAppTopography.current.baseBold,
                    color = YralColors.Neutral50,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    userPrincipalId: String,
    profileImageUrl: String,
    coins: Long,
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
        UserBriefWithBorder(rank, isCurrentUser) {
            UserBriefContent(
                rank,
                userPrincipalId,
                profileImageUrl,
                coins,
                isCurrentUser,
            )
        }
    }
}

@Composable
private fun UserBriefWithBorder(
    rank: Int,
    isCurrentUser: Boolean,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val border = getUserBriefBorder(rank)
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
    rank: Int,
    userPrincipalId: String,
    profileImageUrl: String,
    coins: Long,
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
            UserBriefPositionNumber(rank, isCurrentUser)
        }
        // Player ID column with avatar
        Row(
            modifier = Modifier.weight(USER_DETAIL_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBriefProfileImage(rank, profileImageUrl)
            Spacer(modifier = Modifier.width(8.dp))
            UserBriefProfileName(rank, userPrincipalId, isCurrentUser)
        }
        // Coins column
        Row(
            modifier = Modifier.weight(COIN_BALANCE_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Image(
                painter = painterResource(id = R.drawable.satoshi),
                contentDescription = "image description",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(23.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = coins.toString(),
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
    rank: Int,
    isCurrentUser: Boolean,
) {
    val decoration = getTextDecoration(rank)
    if (decoration != 0 && !isCurrentUser) {
        YralMaskedVectorTextV2(
            text = "#${rank + 1}",
            vectorRes = decoration,
            textStyle = LocalAppTopography.current.baseBold,
            modifier = Modifier.width(21.dp),
            textOverflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    } else {
        Text(
            text = "#${rank + 1}",
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
        UserBriefGradientProfileName(position, name)
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
private fun UserBriefProfileImage(
    rank: Int,
    profileImageUrl: String,
    size: Dp = PROFILE_IMAGE_SIZE.dp,
) {
    Box(modifier = Modifier.wrapContentSize()) {
        YralAsyncImage(
            imageUrl = profileImageUrl,
            size = size,
            backgroundColor = YralColors.ProfilePicBackground,
        )
        val profileImageRing = getProfileImageRing(rank)
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

object LeaderboardScreenConstants {
    const val POSITION_TEXT_WEIGHT = 0.17f
    const val USER_DETAIL_WEIGHT = 0.55f
    const val COIN_BALANCE_WEIGHT = 0.28f
}
