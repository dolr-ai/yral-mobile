package com.yral.shared.features.leaderboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.features.leaderboard.domain.models.RewardCurrency
import com.yral.shared.features.leaderboard.ui.main.LeaderboardHelpers
import com.yral.shared.features.leaderboard.ui.main.LeaderboardMainScreenConstants
import com.yral.shared.libs.CurrencyFormatter
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.leaderboard.generated.resources.Res
import yral_mobile.shared.features.leaderboard.generated.resources.games_won
import yral_mobile.shared.features.leaderboard.generated.resources.player
import yral_mobile.shared.features.leaderboard.generated.resources.position
import yral_mobile.shared.features.leaderboard.generated.resources.rewards
import yral_mobile.shared.features.leaderboard.generated.resources.you
import yral_mobile.shared.libs.designsystem.generated.resources.bitcoin
import yral_mobile.shared.libs.designsystem.generated.resources.yral
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@Composable
fun LeaderboardTableHeader(
    isTrophyVisible: Boolean,
    rewardCurrency: RewardCurrency?,
) {
    val rewardIcon =
        when (rewardCurrency) {
            RewardCurrency.YRAL -> DesignRes.drawable.yral
            RewardCurrency.BTC -> DesignRes.drawable.bitcoin
            else -> null
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (isTrophyVisible) 20.dp else 12.dp,
                    bottom = if (isTrophyVisible) 4.dp else 12.dp,
                ),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.position),
            modifier = Modifier.weight(LeaderboardMainScreenConstants.POSITION_TEXT_WEIGHT),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.player),
            modifier =
                Modifier
                    .weight(LeaderboardMainScreenConstants.USER_DETAIL_WEIGHT)
                    .padding(start = 6.dp),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
        )
        rewardIcon?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(LeaderboardMainScreenConstants.REWARD_WEIGHT),
            ) {
                Text(
                    text = stringResource(Res.string.rewards),
                    style = LocalAppTopography.current.regRegular,
                    textAlign = TextAlign.Center,
                    color = YralColors.NeutralTextSecondary,
                )
                Image(
                    painter = painterResource(rewardIcon),
                    contentDescription = "image description",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = stringResource(Res.string.games_won),
            modifier = Modifier.weight(LeaderboardMainScreenConstants.COIN_BALANCE_WEIGHT),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LeaderboardRow(
    position: Int,
    userPrincipalId: String,
    profileImageUrl: String,
    wins: Long,
    isCurrentUser: Boolean,
    decorateCurrentUser: Boolean = false,
    rewardCurrency: RewardCurrency? = null,
    rewardCurrencyCode: String? = null,
    reward: Double? = null,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(42.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isCurrentUser && decorateCurrentUser) {
                        YralColors.Pink400
                    } else {
                        YralColors.Neutral900
                    },
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        UserBriefWithBorder(position, decorateCurrentUser) {
            UserBriefContent(
                position,
                userPrincipalId,
                profileImageUrl,
                wins,
                isCurrentUser,
                decorateCurrentUser,
                rewardCurrency,
                rewardCurrencyCode,
                reward,
            )
        }
    }
}

@Composable
private fun UserBriefWithBorder(
    position: Int,
    decorateCurrentUser: Boolean,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val border = LeaderboardHelpers.getUserBriefBorder(position)
        if (border != null && !decorateCurrentUser) {
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
    decorateCurrentUser: Boolean,
    rewardCurrency: RewardCurrency?,
    rewardCurrencyCode: String?,
    reward: Double?,
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
            modifier = Modifier.weight(LeaderboardMainScreenConstants.POSITION_TEXT_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBriefPositionNumber(position, decorateCurrentUser)
        }
        // Player ID column with avatar
        Row(
            modifier = Modifier.weight(LeaderboardMainScreenConstants.USER_DETAIL_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBriefProfileImage(position, profileImageUrl)
            Spacer(modifier = Modifier.width(8.dp))
            UserBriefProfileName(position, userPrincipalId, isCurrentUser, decorateCurrentUser)
        }
        // Rewards
        LeaderboardReward(
            modifier = Modifier.weight(LeaderboardMainScreenConstants.REWARD_WEIGHT),
            rewardCurrency = rewardCurrency,
            rewardCurrencyCode = rewardCurrencyCode,
            reward = reward,
            isBackgroundVisible = false,
            horizontalAlignment = Alignment.End,
        )
        // Games won
        Box(
            modifier = Modifier.weight(LeaderboardMainScreenConstants.COIN_BALANCE_WEIGHT),
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
fun UserBriefPositionNumber(
    position: Int,
    decorateCurrentUser: Boolean,
) {
    val decoration = LeaderboardHelpers.getTextDecoration(position)
    if (decoration != null && !decorateCurrentUser) {
        YralMaskedVectorTextV2(
            text = "#$position",
            drawableRes = decoration,
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
    decorateCurrentUser: Boolean,
) {
    val displayName =
        if (isCurrentUser) {
            stringResource(Res.string.you)
        } else {
            name.take(LeaderboardMainScreenConstants.MAX_CHAR_OF_NAME).plus("...")
        }
    if (decorateCurrentUser) {
        Text(
            text = displayName,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.NeutralTextPrimary,
        )
    } else {
        UserBriefGradientProfileName(
            position = position,
            name = displayName,
        )
    }
}

@Composable
private fun UserBriefGradientProfileName(
    position: Int,
    name: String,
) {
    val decoration = LeaderboardHelpers.getTextDecoration(position)
    if (decoration != null) {
        YralMaskedVectorTextV2(
            text = name,
            drawableRes = decoration,
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
    size: Dp = LeaderboardHelpers.PROFILE_IMAGE_SIZE.dp,
) {
    Box(modifier = Modifier.wrapContentSize()) {
        YralAsyncImage(
            imageUrl = profileImageUrl,
            modifier = Modifier.size(size),
            backgroundColor = YralColors.ProfilePicBackground,
        )
        val profileImageRing = LeaderboardHelpers.getProfileImageRing(position)
        if (profileImageRing != null) {
            Image(
                painter = painterResource(profileImageRing),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

@Composable
fun LeaderboardReward(
    modifier: Modifier,
    rewardCurrency: RewardCurrency?,
    rewardCurrencyCode: String?,
    reward: Double?,
    isBackgroundVisible: Boolean,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    val rewardIcon =
        when (rewardCurrency) {
            RewardCurrency.YRAL -> DesignRes.drawable.yral
            RewardCurrency.BTC -> DesignRes.drawable.bitcoin
            else -> null
        }
    val rewardText =
        when (rewardCurrency) {
            RewardCurrency.YRAL -> reward?.toInt()?.toString()
            RewardCurrency.BTC ->
                rewardCurrencyCode?.let { currencyCode -> reward?.toCurrencyString(currencyCode) } ?: ""
            else -> null
        }
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.5.dp, horizontalAlignment),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .width(55.dp)
                .height(24.dp)
                .background(
                    color = if (isBackgroundVisible) YralColors.GameRewardChipBackground else Color.Transparent,
                    shape = RoundedCornerShape(size = 38.dp),
                ).padding(start = 7.dp, top = 3.5.dp, end = 3.5.dp, bottom = 3.5.dp),
    ) {
        rewardText?.let {
            Text(
                text = rewardText,
                style = LocalAppTopography.current.regSemiBold,
                textAlign = TextAlign.End,
                color = if (isBackgroundVisible) YralColors.Neutral950 else YralColors.Neutral50,
            )
            rewardIcon?.let {
                Image(
                    painter = painterResource(rewardIcon),
                    contentDescription = "reward currency",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private fun Double.toCurrencyString(currencyCode: String) =
    CurrencyFormatter()
        .format(
            amount = this,
            currencyCode = currencyCode,
            withCurrencySymbol = true,
            minimumFractionDigits = 2,
            maximumFractionDigits = 2,
        )
