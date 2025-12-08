package com.yral.shared.features.leaderboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.yral.shared.features.leaderboard.ui.main.LeaderboardMainScreenConstants.LEADERBOARD_HEADER_WEIGHTS
import com.yral.shared.features.leaderboard.ui.main.LeaderboardMainScreenConstants.LEADERBOARD_HEADER_WEIGHTS_FOLD
import com.yral.shared.features.leaderboard.ui.main.LeaderboardMainScreenConstants.LEADERBOARD_ROW_WEIGHTS
import com.yral.shared.libs.CurrencyFormatter
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.windowInfo.rememberScreenFoldStateProvider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.leaderboard.generated.resources.Res
import yral_mobile.shared.features.leaderboard.generated.resources.player
import yral_mobile.shared.features.leaderboard.generated.resources.rank
import yral_mobile.shared.features.leaderboard.generated.resources.rewards
import yral_mobile.shared.features.leaderboard.generated.resources.wins
import yral_mobile.shared.features.leaderboard.generated.resources.you
import yral_mobile.shared.libs.designsystem.generated.resources.bitcoin
import yral_mobile.shared.libs.designsystem.generated.resources.yral
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod", "MagicNumber")
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

    val isScreenUnfolded by rememberScreenFoldStateProvider().isScreenUnfoldedFlow.collectAsState(false)
    val headerWeights =
        if (isScreenUnfolded) {
            LEADERBOARD_HEADER_WEIGHTS_FOLD
        } else {
            LEADERBOARD_HEADER_WEIGHTS
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
            text = stringResource(Res.string.rank),
            modifier = Modifier.weight(headerWeights[0]),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.player),
            modifier =
                Modifier
                    .weight(headerWeights[1])
                    .padding(start = 6.dp),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.5.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(headerWeights[2]),
        ) {
            rewardIcon?.let {
                Text(
                    text = stringResource(Res.string.rewards),
                    style = LocalAppTopography.current.regRegular,
                    textAlign = TextAlign.Center,
                    color = YralColors.NeutralTextSecondary,
                )
                Image(
                    painter = painterResource(rewardIcon),
                    contentDescription = "image description",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = stringResource(Res.string.wins),
            modifier = Modifier.weight(headerWeights[3]),
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
    userIdentifier: String,
    profileImageUrl: String,
    wins: Long,
    isCurrentUser: Boolean,
    decorateCurrentUser: Boolean = false,
    rewardCurrency: RewardCurrency? = null,
    rewardCurrencyCode: String? = null,
    reward: Double? = null,
    onClick: (() -> Unit)? = null,
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
        onClick = { onClick?.invoke() },
    ) {
        UserBriefWithBorder(position, decorateCurrentUser) {
            UserBriefContent(
                position,
                userIdentifier,
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

@Suppress("MagicNumber", "UnusedParameter")
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
            modifier = Modifier.weight(LEADERBOARD_ROW_WEIGHTS[0]),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBriefPositionNumber(position, decorateCurrentUser)
        }
        // Player ID column with avatar
        Row(
            modifier = Modifier.weight(LEADERBOARD_ROW_WEIGHTS[1]),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // UserBriefProfileImage(position, profileImageUrl)
            // Spacer(modifier = Modifier.width(8.dp))
            UserBriefProfileName(position, userPrincipalId, isCurrentUser, decorateCurrentUser)
        }
        // Rewards
        LeaderboardReward(
            modifier = Modifier.weight(LEADERBOARD_ROW_WEIGHTS[2]),
            rewardCurrency = rewardCurrency,
            rewardCurrencyCode = rewardCurrencyCode,
            reward = reward,
            isBackgroundVisible = false,
            horizontalAlignment = Alignment.End,
        )
        // Games won
        Box(
            modifier = Modifier.weight(LEADERBOARD_ROW_WEIGHTS[3]),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = formatAbbreviation(wins),
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
    val formattedPosition = formatAbbreviation(position)
    if (decoration != null && !decorateCurrentUser) {
        YralMaskedVectorTextV2(
            text = "#$formattedPosition",
            drawableRes = decoration,
            textStyle = LocalAppTopography.current.baseBold,
            modifier = Modifier.width(21.dp),
            textOverflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    } else {
        Text(
            text = "#$formattedPosition",
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
                rewardCurrencyCode?.let { currencyCode -> reward?.toCurrencyString(currencyCode) }
            else -> null
        }
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.5.dp, horizontalAlignment),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
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
                    contentScale = ContentScale.FillBounds,
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
