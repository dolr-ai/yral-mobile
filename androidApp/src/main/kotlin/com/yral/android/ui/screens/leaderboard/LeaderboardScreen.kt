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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.BRONZE_TROPHY_SIZE
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.COIN_BALANCE_WEIGHT
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.GOLDEN_TROPHY_SIZE
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.POSITION_TEXT_WEIGHT
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.PROFILE_IMAGE_SIZE
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.SILVER_TROPHY_SIZE
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.TROPHY_GALLERY_SIZE
import com.yral.android.ui.screens.leaderboard.LeaderboardScreenConstants.USER_DETAIL_WEIGHT
import com.yral.android.ui.widgets.YralLoader
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.android.ui.widgets.YralMaskedVectorTextV2
import com.yral.shared.features.game.domain.models.CurrentUserInfo
import com.yral.shared.features.game.domain.models.LeaderboardItem
import com.yral.shared.features.game.viewmodel.LeaderBoardViewModel

@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    viewModel: LeaderBoardViewModel,
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
    ) {
        // Trophies
        TrophyGallery(state.leaderboard)
        // Table Header
        LeaderboardTableHeader()
        Spacer(modifier = Modifier.height(8.dp))
        // Content
        when {
            state.isLoading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    YralLoader()
                }
            }

            state.error != null -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Error: ${state.error}",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            else -> {
                LeaderboardContent(
                    leaderboard = state.leaderboard,
                    currentUser = state.currentUser,
                )
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
        )
        Text(
            text = stringResource(R.string.player_id),
            modifier = Modifier.weight(USER_DETAIL_WEIGHT),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
        )
        Text(
            text = stringResource(R.string.total_sats),
            modifier = Modifier.weight(COIN_BALANCE_WEIGHT),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun TrophyGallery(leaderboard: List<LeaderboardItem>) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height((TROPHY_GALLERY_SIZE * screenHeight).dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        YralLottieAnimation(
            modifier = Modifier.fillMaxSize(),
            rawRes = R.raw.leaderboard_star,
        )
        // Header
        Column {
            Text(
                text = stringResource(R.string.leaderboard),
                style = LocalAppTopography.current.xlBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            if (leaderboard.size > 3) {
                Spacer(Modifier.weight(4f))
                TrophyImages(leaderboard)
                Spacer(Modifier.weight(1f))
                TrophyDetails(leaderboard)
                Spacer(Modifier.weight(3f))
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
            position = 1,
            profileImageUrl = leaderboard[1].profileImage,
            profileImageSize = SILVER_TROPHY_SIZE.dp,
            trophyResource = R.drawable.silver_trophy,
        )
        Trophy(
            position = 0,
            profileImageUrl = leaderboard[0].profileImage,
            profileImageSize = GOLDEN_TROPHY_SIZE.dp,
            trophyResource = R.drawable.golden_trophy,
        )
        Trophy(
            position = 2,
            profileImageUrl = leaderboard[2].profileImage,
            profileImageSize = BRONZE_TROPHY_SIZE.dp,
            trophyResource = R.drawable.bronze_trophy,
        )
    }
}

@Composable
private fun ColumnScope.TrophyDetails(leaderboard: List<LeaderboardItem>) {
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        TrophyDetailsItem(
            userPrincipalId = leaderboard[1].userPrincipalId,
            coins = leaderboard[1].coins,
        )
        TrophyDetailsItem(
            userPrincipalId = leaderboard[0].userPrincipalId,
            coins = leaderboard[0].coins,
        )
        TrophyDetailsItem(
            userPrincipalId = leaderboard[2].userPrincipalId,
            coins = leaderboard[2].coins,
        )
    }
}

@Composable
private fun Trophy(
    position: Int,
    profileImageUrl: String,
    profileImageSize: Dp,
    trophyResource: Int,
) {
    Column {
        Box(
            modifier =
                Modifier
                    .offset(y = profileImageSize / 2 + 8.dp)
                    .zIndex(1f)
                    .align(Alignment.CenterHorizontally),
        ) {
            UserBriefProfileImage(
                profileImageUrl,
                position = position,
                size = profileImageSize,
            )
        }
        Image(
            painter = painterResource(id = trophyResource),
            contentDescription = "image description",
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun TrophyDetailsItem(
    userPrincipalId: String,
    coins: Long,
) {
    Column(
        modifier = Modifier.widthIn(max = 93.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = userPrincipalId,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.NeutralTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
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

@Composable
private fun LeaderboardContent(
    leaderboard: List<LeaderboardItem>,
    currentUser: CurrentUserInfo?,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Show current user first if available
        currentUser?.let { user ->
            item {
                LeaderboardRow(
                    position = user.leaderboardPosition,
                    userPrincipalId = user.userPrincipalId,
                    profileImageUrl = user.profileImageUrl,
                    coins = user.coins,
                    isCurrentUser = true,
                )
            }
        }
        // Show other leaderboard items
        items(leaderboard) { item ->
            LeaderboardRow(
                position = leaderboard.indexOf(item),
                userPrincipalId = item.userPrincipalId,
                profileImageUrl = item.profileImage,
                coins = item.coins,
                isCurrentUser = false,
            )
        }
    }
}

@Composable
private fun LeaderboardRow(
    position: Int,
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
        UserBriefWithBorder(position) {
            UserBriefContent(
                position,
                userPrincipalId,
                profileImageUrl,
                coins,
                isCurrentUser,
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun UserBriefWithBorder(
    position: Int,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (position == 0 || position == 1 || position == 2) {
            Image(
                painter =
                    painterResource(
                        when (position) {
                            0 -> R.drawable.golden_border
                            1 -> R.drawable.silver_border
                            2 -> R.drawable.bronze_border
                            else -> 0
                        },
                    ),
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
    coins: Long,
    isCurrentUser: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
    ) {
        // Position column
        Row(
            modifier = Modifier.weight(POSITION_TEXT_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#$position",
                style = LocalAppTopography.current.baseBold,
                color = YralColors.Neutral50,
                textAlign = TextAlign.Center,
            )
        }
        // Player ID column with avatar
        Row(
            modifier = Modifier.weight(USER_DETAIL_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBriefProfileImage(profileImageUrl, position)
            Spacer(modifier = Modifier.width(12.dp))
            UserBriefProfileName(position, userPrincipalId, isCurrentUser)
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
            )
        }
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

@Suppress("MagicNumber")
@Composable
private fun UserBriefGradientProfileName(
    position: Int,
    name: String,
) {
    when (position) {
        1 -> {
            YralMaskedVectorTextV2(
                text = name,
                vectorRes = R.drawable.golden_gradient,
                textStyle = LocalAppTopography.current.baseMedium,
                maxLines = 1,
                textOverflow = TextOverflow.Ellipsis,
            )
        }

        2 -> {
            YralMaskedVectorTextV2(
                text = name,
                vectorRes = R.drawable.silver_gradient,
                textStyle = LocalAppTopography.current.baseMedium,
                maxLines = 1,
                textOverflow = TextOverflow.Ellipsis,
            )
        }

        3 -> {
            YralMaskedVectorTextV2(
                text = name,
                vectorRes = R.drawable.bronze_gradient,
                textStyle = LocalAppTopography.current.baseMedium,
                maxLines = 1,
                textOverflow = TextOverflow.Ellipsis,
            )
        }

        else -> {
            Text(
                text = name,
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UserBriefProfileImage(
    profileImageUrl: String,
    position: Int,
    size: Dp = PROFILE_IMAGE_SIZE.dp,
) {
    val shape = RoundedCornerShape(size = size)
    Box(modifier = Modifier.wrapContentSize()) {
        AsyncImage(
            model = profileImageUrl,
            contentDescription = "User picture",
            contentScale = ContentScale.FillBounds,
            modifier =
                Modifier
                    .clip(shape)
                    .size(size)
                    .background(
                        color = YralColors.ProfilePicBackground,
                        shape = shape,
                    ),
        )
        if (position == 0 || position == 1 || position == 2) {
            Image(
                painter =
                    painterResource(
                        when (position) {
                            0 -> R.drawable.golden_ring
                            1 -> R.drawable.silver_ring
                            2 -> R.drawable.bronze_ring
                            else -> 0
                        },
                    ),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

object LeaderboardScreenConstants {
    const val POSITION_TEXT_WEIGHT = 0.5f
    const val USER_DETAIL_WEIGHT = 2f
    const val COIN_BALANCE_WEIGHT = 1f
    const val PROFILE_IMAGE_SIZE = 25f
    const val TROPHY_GALLERY_SIZE = 0.45
    const val SILVER_TROPHY_SIZE = 48
    const val BRONZE_TROPHY_SIZE = 46
    const val GOLDEN_TROPHY_SIZE = 64
}
