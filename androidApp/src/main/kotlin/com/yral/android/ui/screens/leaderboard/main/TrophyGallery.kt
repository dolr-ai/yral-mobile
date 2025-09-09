package com.yral.android.ui.screens.leaderboard.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.leaderboard.UserBriefProfileImage
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.MAX_USERS_PRINCIPAL_LENGTH
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.MAX_USERS_WITH_DUPLICATE_RANK
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.POS_BRONZE
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.POS_GOLD
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.POS_SILVER
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.getTrophyImageHeight
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.getTrophyImageOffset
import com.yral.android.ui.screens.leaderboard.main.LeaderboardHelpers.getTrophyImageWidth
import com.yral.android.ui.widgets.YralLottieAnimation
import com.yral.shared.features.game.data.models.LeaderboardMode
import com.yral.shared.features.game.domain.models.LeaderboardItem

@Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun TrophyGallery(
    isLoading: Boolean,
    leaderboard: List<LeaderboardItem>,
    countDownMs: Long?,
    blinkCountDown: Boolean,
    selectedMode: LeaderboardMode,
    selectMode: (LeaderboardMode) -> Unit,
    openHistory: () -> Unit,
) {
    val leaderboardBG =
        when (selectedMode) {
            LeaderboardMode.DAILY -> R.drawable.yellow_leaderboard
            LeaderboardMode.ALL_TIME -> R.drawable.purple_leaderboard
        }
    val trophyPaddingTop =
        if ((selectedMode.showCountDown && countDownMs != null) || isLoading) {
            28.dp
        } else {
            56.dp
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(leaderboardBG),
                    contentScale = ContentScale.FillBounds,
                ),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (leaderboard.isNotEmpty()) {
            val lottie =
                when (selectedMode) {
                    LeaderboardMode.DAILY -> R.raw.yellow_rays
                    LeaderboardMode.ALL_TIME -> R.raw.purple_rays
                }
            key(lottie) {
                YralLottieAnimation(
                    modifier = Modifier.matchParentSize(),
                    rawRes = lottie,
                    iterations = 1,
                    contentScale = ContentScale.FillBounds,
                )
            }
        }
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
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
            if (leaderboard.size > 3) {
                Spacer(Modifier.height(trophyPaddingTop))
                TrophyImages(leaderboard)
                Spacer(Modifier.height(12.dp))
                TrophyDetails(leaderboard, selectedMode)
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
            position = POS_SILVER,
            profileImageUrl = getProfileImageForTrophy(1, leaderboard),
            trophyResource = R.drawable.silver_trophy,
        )
        Trophy(
            position = POS_GOLD,
            profileImageUrl = getProfileImageForTrophy(0, leaderboard),
            trophyResource = R.drawable.golden_trophy,
        )
        Trophy(
            position = POS_BRONZE,
            profileImageUrl = getProfileImageForTrophy(2, leaderboard),
            trophyResource = R.drawable.bronze_trophy,
        )
    }
}

private fun getProfileImageForTrophy(
    position: Int,
    leaderboard: List<LeaderboardItem>,
): String =
    if (leaderboard.filter { it.position == position }.size == 1) {
        // users[0].profileImage
        ""
    } else {
        ""
    }

@Composable
private fun ColumnScope.TrophyDetails(
    leaderboard: List<LeaderboardItem>,
    leaderboardMode: LeaderboardMode,
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
            )
            TrophyDetailsItem(
                position = POS_GOLD,
                userPrincipalId = posGold.first,
                gamesWon = posGold.second,
                lines = lines,
                leaderboardMode = leaderboardMode,
            )
            TrophyDetailsItem(
                position = POS_BRONZE,
                userPrincipalId = posBronze.first,
                gamesWon = posBronze.second,
                lines = lines,
                leaderboardMode = leaderboardMode,
            )
        }
    }
}

private fun getTrophyDetailItem(
    position: Int,
    leaderboard: List<LeaderboardItem>,
): Pair<String, Long> {
    val users = leaderboard.filter { it.position == position }
    return if (users.isNotEmpty()) {
        getTrophyDetailsUserTexts(users) to users[0].wins
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
    position: Int,
    profileImageUrl: String,
    trophyResource: Int,
) {
    val width = getTrophyImageWidth(position)
    val height = getTrophyImageHeight(position)
    val offset =
        getTrophyImageOffset(
            position = position,
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
                position = position,
                profileImageUrl = profileImageUrl,
                size = width,
            )
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
                text = stringResource(R.string.games_won),
                style = gameWonStyleDes,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.onSizeChanged { textSize2 = it / 2 },
            )
        }
    }
}
