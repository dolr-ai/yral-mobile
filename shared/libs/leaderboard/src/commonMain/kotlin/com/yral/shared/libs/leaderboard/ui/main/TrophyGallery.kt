package com.yral.shared.libs.leaderboard.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.leaderboard.model.LeaderboardEntry
import com.yral.shared.libs.leaderboard.model.LeaderboardMode
import com.yral.shared.libs.leaderboard.model.RewardCurrency
import com.yral.shared.libs.leaderboard.ui.LeaderboardReward
import com.yral.shared.libs.leaderboard.ui.UserBriefProfileImage
import com.yral.shared.libs.leaderboard.ui.main.LeaderboardHelpers.POS_BRONZE
import com.yral.shared.libs.leaderboard.ui.main.LeaderboardHelpers.POS_GOLD
import com.yral.shared.libs.leaderboard.ui.main.LeaderboardHelpers.POS_SILVER
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.leaderboard.generated.resources.Res
import yral_mobile.shared.libs.leaderboard.generated.resources.bronze_trophy
import yral_mobile.shared.libs.leaderboard.generated.resources.games_won
import yral_mobile.shared.libs.leaderboard.generated.resources.golden_trophy
import yral_mobile.shared.libs.leaderboard.generated.resources.silver_trophy

@Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun TrophyGallery(
    isLoading: Boolean,
    leaderboard: List<LeaderboardEntry>,
    countDownMs: Long?,
    blinkCountDown: Boolean,
    selectedMode: LeaderboardMode,
    selectMode: (LeaderboardMode) -> Unit,
    openHistory: () -> Unit,
    isTrophyVisible: Boolean,
    rewardCurrency: RewardCurrency? = null,
    rewardCurrencyCode: String? = null,
    rewardsTable: Map<Int, Double>? = null,
) {
    val trophyPaddingTop =
        when {
            isLoading || leaderboard.isEmpty() -> 0.dp
            selectedMode.showCountDown -> 16.dp
            !selectedMode.showCountDown -> 50.dp
            else -> 0.dp
        }
    val lottie =
        when (selectedMode) {
            LeaderboardMode.DAILY -> LottieRes.YELLOW_RAYS
            LeaderboardMode.ALL_TIME -> LottieRes.PURPLE_RAYS
        }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = leaderboard.isNotEmpty() && isTrophyVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize(),
        ) {
            key(lottie) {
                YralLottieAnimation(
                    modifier = Modifier.fillMaxSize(),
                    rawRes = lottie,
                    iterations = 1,
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 23.dp)) {
            LeaderboardModeSelection(selectedMode, selectMode)
            Row {
                if (selectedMode.showCountDown) {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .offset(x = 28.dp)
                                .padding(top = 28.dp),
                    ) {
                        countDownMs?.let { LeaderboardCountdown(countDownMs, blinkCountDown) }
                    }
                }
                if (selectedMode.showHistory) {
                    LeaderboardHistoryIcon(
                        modifier =
                            Modifier
                                .padding(end = 12.dp)
                                .offset(y = countDownMs?.let { 6.dp } ?: 16.dp)
                                .align(Alignment.Bottom)
                                .clickable { openHistory() },
                    )
                }
            }
            if (selectedMode.showCountDown && countDownMs == null) {
                Spacer(modifier = Modifier.height(11.dp))
            }
            AnimatedVisibility(visible = isTrophyVisible) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = trophyPaddingTop),
                ) {
                    TrophyImages(leaderboard, rewardCurrency, rewardCurrencyCode, rewardsTable)
                    TrophyDetails(leaderboard, selectedMode, isLoading)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TrophyImages(
    leaderboard: List<LeaderboardEntry>,
    rewardCurrency: RewardCurrency?,
    rewardCurrencyCode: String?,
    rewardsTable: Map<Int, Double>?,
) {
    Row(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(42.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Trophy(
            position = POS_SILVER,
            profileImageUrl = getProfileImageForTrophy(1, leaderboard),
            trophyResource = Res.drawable.silver_trophy,
            rewardCurrency = rewardCurrency,
            rewardCurrencyCode = rewardCurrencyCode,
            rewardsTable = rewardsTable,
        )
        Trophy(
            position = POS_GOLD,
            profileImageUrl = getProfileImageForTrophy(0, leaderboard),
            trophyResource = Res.drawable.golden_trophy,
            rewardCurrency = rewardCurrency,
            rewardCurrencyCode = rewardCurrencyCode,
            rewardsTable = rewardsTable,
        )
        Trophy(
            position = POS_BRONZE,
            profileImageUrl = getProfileImageForTrophy(2, leaderboard),
            trophyResource = Res.drawable.bronze_trophy,
            rewardCurrency = rewardCurrency,
            rewardCurrencyCode = rewardCurrencyCode,
            rewardsTable = rewardsTable,
        )
    }
}

private fun getProfileImageForTrophy(
    position: Int,
    leaderboard: List<LeaderboardEntry>,
): String =
    if (leaderboard.filter { it.position == position }.size == 1) {
        // users[0].profileImage
        ""
    } else {
        ""
    }

@Composable
private fun ColumnScope.TrophyDetails(
    leaderboard: List<LeaderboardEntry>,
    leaderboardMode: LeaderboardMode,
    isLoading: Boolean,
) {
    Column(modifier = Modifier.align(Alignment.CenterHorizontally)) {
        val posGold by remember(leaderboard) { mutableStateOf(getTrophyDetailItem(POS_GOLD, leaderboard)) }
        val posSilver by remember(leaderboard) { mutableStateOf(getTrophyDetailItem(POS_SILVER, leaderboard)) }
        val posBronze by remember(leaderboard) { mutableStateOf(getTrophyDetailItem(POS_BRONZE, leaderboard)) }
        val lines =
            if (setOf(posGold.first, posSilver.first, posBronze.first).any { it.contains(",") }) {
                2
            } else {
                1
            }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            TrophyDetailsItem(
                position = POS_SILVER,
                userPrincipalId = posSilver.first,
                gamesWon = posSilver.second,
                lines = lines,
                leaderboardMode = leaderboardMode,
                isLoading = isLoading,
            )
            TrophyDetailsItem(
                position = POS_GOLD,
                userPrincipalId = posGold.first,
                gamesWon = posGold.second,
                lines = lines,
                leaderboardMode = leaderboardMode,
                isLoading = isLoading,
            )
            TrophyDetailsItem(
                position = POS_BRONZE,
                userPrincipalId = posBronze.first,
                gamesWon = posBronze.second,
                lines = lines,
                leaderboardMode = leaderboardMode,
                isLoading = isLoading,
            )
        }
    }
}

private fun getTrophyDetailItem(
    position: Int,
    leaderboard: List<LeaderboardEntry>,
): Pair<String, Long> {
    val users = leaderboard.filter { it.position == position }
    return if (users.isNotEmpty()) {
        getTrophyDetailsUserTexts(users) to users[0].wins
    } else {
        "" to -1
    }
}

private fun getTrophyDetailsUserTexts(user: List<LeaderboardEntry>): String =
    when (user.size) {
        1 -> user[0].username
        else ->
            user
                .take(LeaderboardHelpers.MAX_USERS_WITH_DUPLICATE_RANK)
                .joinToString(", ") {
                    it.username.take(LeaderboardHelpers.MAX_USERS_PRINCIPAL_LENGTH) + "..."
                }
    }

@Composable
private fun Trophy(
    position: Int,
    profileImageUrl: String,
    trophyResource: DrawableResource,
    rewardCurrency: RewardCurrency?,
    rewardCurrencyCode: String?,
    rewardsTable: Map<Int, Double>?,
) {
    val width = LeaderboardHelpers.getTrophyImageWidth(position)
    val height = LeaderboardHelpers.getTrophyImageHeight(position)
    val offset =
        LeaderboardHelpers.getTrophyImageOffset(
            position = position,
            isProfileImageVisible = profileImageUrl.isNotEmpty(),
        )
    val rewardOffset = LeaderboardHelpers.getTrophyRewardOffset(position = position)
    Box(
        modifier =
            Modifier
                .height(height + offset),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(trophyResource),
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
                position = position,
                profileImageUrl = profileImageUrl,
                size = width,
            )
        }
        rewardsTable?.get(position)?.let {
            if (rewardCurrency != null) {
                LeaderboardReward(
                    rewardCurrency = rewardCurrency,
                    rewardCurrencyCode = rewardCurrencyCode,
                    reward = it,
                    isBackgroundVisible = true,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(0, rewardOffset.roundToPx()) },
                )
            }
        }
    }
}

@Suppress("UnusedParameter")
@Composable
private fun TrophyDetailsItem(
    position: Int,
    userPrincipalId: String,
    lines: Int,
    gamesWon: Long,
    leaderboardMode: LeaderboardMode,
    isLoading: Boolean,
) {
    val principalColor =
        when (leaderboardMode) {
            LeaderboardMode.DAILY -> YralColors.NeutralTextTertiary
            LeaderboardMode.ALL_TIME -> YralColors.NeutralTextPrimary
        }
    var textSize1 by remember { mutableStateOf(IntSize.Zero) }
//    val brush1 = remember(position, textSize1) { getBrush(position, textSize1) }
//    val gamesWonStyle = LocalAppTopography.current.baseBold.copy(brush = brush1)
    val gamesWonStyle =
        when (leaderboardMode) {
            LeaderboardMode.DAILY -> LocalAppTopography.current.baseBold.copy(color = principalColor)
            LeaderboardMode.ALL_TIME -> LocalAppTopography.current.baseBold.copy(color = principalColor)
        }
    var textSize2 by remember { mutableStateOf(IntSize.Zero) }
//    val brush2 = remember(position, textSize2) { getBrush(position, textSize2) }
//    val gameWonStyleDes = LocalAppTopography.current.baseBold.copy(brush = brush2, color = YralColors.Neutral50)
    val gameWonStyleDes =
        when (leaderboardMode) {
            LeaderboardMode.DAILY -> LocalAppTopography.current.baseBold.copy(color = principalColor)
            LeaderboardMode.ALL_TIME -> LocalAppTopography.current.baseBold.copy(color = principalColor)
        }
    if (isLoading) {
        Box(Modifier.width(85.dp)) {
            YralLoader(size = 28.dp)
        }
    } else {
        Column(
            modifier = Modifier.width(93.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = userPrincipalId,
                style = LocalAppTopography.current.baseMedium,
                color = principalColor,
                minLines = lines,
                maxLines = lines,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            if (gamesWon >= 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = gamesWon.toString(),
                    style = gamesWonStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.onSizeChanged { textSize1 = it },
                )
                Text(
                    text = stringResource(Res.string.games_won),
                    style = gameWonStyleDes,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.onSizeChanged { textSize2 = it / 2 },
                )
            }
        }
    }
}
