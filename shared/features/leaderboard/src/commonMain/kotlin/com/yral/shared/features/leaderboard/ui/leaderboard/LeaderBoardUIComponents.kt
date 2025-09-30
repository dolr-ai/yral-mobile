package com.yral.shared.features.leaderboard.ui.leaderboard

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardHelpers.PROFILE_IMAGE_SIZE
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardHelpers.getProfileImageRing
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardHelpers.getTextDecoration
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardHelpers.getUserBriefBorder
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardMainScreenConstants.COIN_BALANCE_WEIGHT
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardMainScreenConstants.MAX_CHAR_OF_NAME
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardMainScreenConstants.POSITION_TEXT_WEIGHT
import com.yral.shared.features.leaderboard.ui.leaderboard.main.LeaderboardMainScreenConstants.USER_DETAIL_WEIGHT
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.leaderboard.generated.resources.Res
import yral_mobile.shared.features.leaderboard.generated.resources.games_won
import yral_mobile.shared.features.leaderboard.generated.resources.player_id
import yral_mobile.shared.features.leaderboard.generated.resources.position
import yral_mobile.shared.features.leaderboard.generated.resources.you

@Composable
fun LeaderboardTableHeader(isTrophyVisible: Boolean) {
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
            modifier = Modifier.weight(POSITION_TEXT_WEIGHT),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.player_id),
            modifier =
                Modifier
                    .weight(USER_DETAIL_WEIGHT)
                    .padding(start = PROFILE_IMAGE_SIZE.dp + 8.dp),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
        )
        Text(
            text = stringResource(Res.string.games_won),
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
fun LeaderboardRow(
    position: Int,
    userPrincipalId: String,
    profileImageUrl: String,
    wins: Long,
    isCurrentUser: Boolean,
    decorateCurrentUser: Boolean = false,
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
        val border = getUserBriefBorder(position)
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
            UserBriefPositionNumber(position, decorateCurrentUser)
        }
        // Player ID column with avatar
        Row(
            modifier = Modifier.weight(USER_DETAIL_WEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBriefProfileImage(position, profileImageUrl)
            Spacer(modifier = Modifier.width(8.dp))
            UserBriefProfileName(position, userPrincipalId, isCurrentUser, decorateCurrentUser)
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
fun UserBriefPositionNumber(
    position: Int,
    decorateCurrentUser: Boolean,
) {
    val decoration = getTextDecoration(position)
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
            name.take(MAX_CHAR_OF_NAME).plus("...")
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
    val decoration = getTextDecoration(position)
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
    size: Dp = PROFILE_IMAGE_SIZE.dp,
) {
    Box(modifier = Modifier.wrapContentSize()) {
        YralAsyncImage(
            imageUrl = profileImageUrl,
            modifier = Modifier.size(size),
            backgroundColor = YralColors.ProfilePicBackground,
        )
        val profileImageRing = getProfileImageRing(position)
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
