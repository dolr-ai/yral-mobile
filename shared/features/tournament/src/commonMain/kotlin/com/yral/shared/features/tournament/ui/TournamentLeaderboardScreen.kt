package com.yral.shared.features.tournament.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.yral.shared.analytics.events.SubscriptionEntryPoint
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.features.subscriptions.nav.SubscriptionNudgeContent
import com.yral.shared.features.subscriptions.ui.components.BoltIcon
import com.yral.shared.features.tournament.domain.model.LeaderboardRow
import com.yral.shared.features.tournament.domain.model.TournamentType
import com.yral.shared.features.tournament.viewmodel.TournamentLeaderboardViewModel
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.component.features.ProfileImageView
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.leaderboard.model.RewardCurrency
import com.yral.shared.libs.leaderboard.ui.LeaderboardReward
import com.yral.shared.libs.leaderboard.ui.main.LeaderboardHelpers
import com.yral.shared.libs.leaderboard.ui.main.LeaderboardUiConstants
import com.yral.shared.libs.leaderboard.ui.main.LeaderboardUiConstants.TOURNAMENT_LEADERBOARD_HEADER_WEIGHTS
import com.yral.shared.rust.service.domain.models.SubscriptionPlan
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.propicFromPrincipal
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.bitcoin
import yral_mobile.shared.features.tournament.generated.resources.ic_calendar
import yral_mobile.shared.features.tournament.generated.resources.ic_users
import yral_mobile.shared.features.tournament.generated.resources.tournament_leaderboard_diamonds
import yral_mobile.shared.features.tournament.generated.resources.tournament_leaderboard_player
import yral_mobile.shared.features.tournament.generated.resources.tournament_leaderboard_rank
import yral_mobile.shared.features.tournament.generated.resources.tournament_leaderboard_rewards
import yral_mobile.shared.features.tournament.generated.resources.tournament_leaderboard_title
import yral_mobile.shared.features.tournament.generated.resources.winner_amount_prefix
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.leaderboard.generated.resources.bronze_trophy
import yral_mobile.shared.libs.leaderboard.generated.resources.golden_trophy
import yral_mobile.shared.libs.leaderboard.generated.resources.silver_trophy
import yral_mobile.shared.libs.leaderboard.generated.resources.yellow_leaderboard
import yral_mobile.shared.libs.leaderboard.generated.resources.you
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes
import yral_mobile.shared.libs.leaderboard.generated.resources.Res as LeaderboardRes

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun TournamentLeaderboardScreen(
    tournamentId: String,
    tournamentTitle: String,
    tournamentType: TournamentType = TournamentType.SMILEY,
    showResult: Boolean = false,
    onBack: () -> Unit,
    onOpenProfile: (CanisterData) -> Unit,
    subscriptionCoordinator: SubscriptionCoordinator,
    viewModel: TournamentLeaderboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val showResultOverlay = state.showResultOverlay

    LaunchedEffect(tournamentId, showResult) {
        viewModel.initShowResultOverlay(showResult)
        viewModel.loadLeaderboard(tournamentId, tournamentType)
    }

    LaunchedEffect(viewModel) {
        viewModel.eventsFlow.collectLatest { event ->
            when (event) {
                is TournamentLeaderboardViewModel.Event.OpenProfile -> onOpenProfile(event.canisterData)
                is TournamentLeaderboardViewModel.Event.ShowSubscriptionNudge ->
                    subscriptionCoordinator.showSubscriptionNudge(
                        SubscriptionNudgeContent(
                            title = null,
                            description = null,
                            topContent = { BoltIcon() },
                            entryPoint = SubscriptionEntryPoint.TOURNAMENT,
                        ),
                    )
            }
        }
    }

    val listState = rememberLazyListState()
    var isTrophyVisible by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(it),
        ) {
            if (!showResultOverlay) {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(
                                object : NestedScrollConnection {
                                    override suspend fun onPostFling(
                                        consumed: Velocity,
                                        available: Velocity,
                                    ): Velocity {
                                        isTrophyVisible = consumed.y > 0
                                        return super.onPostFling(consumed, available)
                                    }
                                },
                            ),
                ) {
                    stickyHeader {
                        TournamentLeaderboardHeader(
                            tournamentTitle = tournamentTitle,
                            participantsLabel = state.participantsLabel.orEmpty(),
                            scheduleLabel = state.scheduleLabel.orEmpty(),
                            leaderboard = state.leaderboard,
                            prizeMap = state.prizeMap,
                            onBack = onBack,
                            isTrophyVisible = isTrophyVisible,
                        )
                    }

                    if (state.leaderboard.isNotEmpty()) {
                        item {
                            TournamentLeaderboardTableHeader(isTrophyVisible = isTrophyVisible)
                        }
                    }

                    val currentUser = state.currentUser
                    val profileDetailsMap = state.profileDetailsByPrincipalId
                    if (currentUser != null) {
                        item {
                            TournamentLeaderboardRow(
                                row = currentUser,
                                isCurrentUser = true,
                                fallbackPrize = state.prizeMap[currentUser.position],
                                profileImageUrl = profileImageUrlFor(currentUser.principalId, profileDetailsMap),
                                isPro = isProFor(currentUser.principalId, profileDetailsMap),
                                onClick = { viewModel.onUserClick(currentUser) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }

                    items(state.leaderboard) { row ->
                        TournamentLeaderboardRow(
                            row = row,
                            isCurrentUser = false,
                            fallbackPrize = state.prizeMap[row.position],
                            profileImageUrl = profileImageUrlFor(row.principalId, profileDetailsMap),
                            isPro = isProFor(row.principalId, profileDetailsMap),
                            onClick = { viewModel.onUserClick(row) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
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

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }

            val currentUser = state.currentUser
            if (showResultOverlay && !state.isLoading && currentUser != null) {
                val rank = currentUser.position
                val prizeAmountValue = currentUser.prize ?: state.prizeMap[rank] ?: 0
                val prizeAmount =
                    stringResource(Res.string.winner_amount_prefix) + prizeAmountValue.toString()
                val totalPrizePoolAmount =
                    stringResource(Res.string.winner_amount_prefix) + state.prizeMap.maxOf { it.value }.toString()
                val shouldShowWinner = rank > 0 && (currentUser.prize != null || state.prizeMap.containsKey(rank))
                val dismissResult = { viewModel.dismissResultOverlay() }
                val closeResult = { onBack() }

                // Track result screen viewed
                LaunchedEffect(shouldShowWinner, rank) {
                    viewModel.trackResultScreenViewed(
                        tournamentId = tournamentId,
                        tournamentType = tournamentType,
                        isWin = shouldShowWinner,
                        finalScore = currentUser.diamonds,
                        rank = rank,
                    )
                }

                if (shouldShowWinner) {
                    TournamentWinnerScreen(
                        prizeAmount = prizeAmount,
                        rank = rank,
                        onClose = closeResult,
                        onViewLeaderboard = dismissResult,
                    )
                } else {
                    TournamentFailScreen(
                        totalPrizePoolAmount = totalPrizePoolAmount,
                        onClose = closeResult,
                        onViewLeaderboard = dismissResult,
                    )
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

@Suppress("MagicNumber")
@Composable
private fun TournamentLeaderboardHeader(
    tournamentTitle: String,
    participantsLabel: String,
    scheduleLabel: String,
    leaderboard: List<LeaderboardRow>,
    prizeMap: Map<Int, Int>,
    onBack: () -> Unit,
    isTrophyVisible: Boolean,
) {
    val gradient = Brush.verticalGradient(colors = LeaderboardUiConstants.YELLOW_BRUSH)
    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(LeaderboardRes.drawable.yellow_leaderboard),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = if (isTrophyVisible) Alignment.Center else Alignment.TopCenter,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(gradient),
        )
        AnimatedVisibility(
            visible = isTrophyVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize(),
        ) {
            YralLottieAnimation(
                modifier = Modifier.fillMaxSize(),
                rawRes = LottieRes.YELLOW_RAYS,
                iterations = 1,
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 23.dp),
        ) {
            HeaderBar(title = tournamentTitle, onBack = onBack)
            Spacer(modifier = Modifier.height(8.dp))
            TournamentMetaRow(participantsLabel = participantsLabel, scheduleLabel = scheduleLabel)
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(visible = isTrophyVisible) {
                TournamentPodium(leaderboard, prizeMap)
            }
        }
    }
}

@Composable
private fun HeaderBar(
    title: String,
    onBack: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "back",
            colorFilter = ColorFilter.tint(YralColors.Neutral950),
            modifier =
                Modifier
                    .size(24.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onBack() },
        )
        Text(
            text = title.ifBlank { stringResource(Res.string.tournament_leaderboard_title) },
            style = LocalAppTopography.current.xlBold,
            color = YralColors.Neutral950,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun TournamentMetaRow(
    participantsLabel: String,
    scheduleLabel: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(top = 6.dp, bottom = 16.dp)
                .background(
                    color = Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(999.dp),
                ).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_users),
                contentDescription = null,
                colorFilter = ColorFilter.tint(YralColors.Neutral950),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = participantsLabel,
                style = LocalAppTopography.current.regMedium,
                color = YralColors.Neutral950,
            )
        }
        Text(
            text = "|",
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral950,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_calendar),
                contentDescription = null,
                colorFilter = ColorFilter.tint(YralColors.Neutral950),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = scheduleLabel,
                style = LocalAppTopography.current.regMedium,
                color = YralColors.Neutral950,
            )
        }
    }
}

@Composable
private fun TournamentPodium(
    leaderboard: List<LeaderboardRow>,
    prizeMap: Map<Int, Int>,
) {
    val first = leaderboard.firstOrNull { it.position == LeaderboardHelpers.POS_GOLD }
    val second = leaderboard.firstOrNull { it.position == LeaderboardHelpers.POS_SILVER }
    val third = leaderboard.firstOrNull { it.position == LeaderboardHelpers.POS_BRONZE }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TournamentPodiumSlot {
            TournamentPodiumEntry(position = LeaderboardHelpers.POS_SILVER, row = second, prize = prizeMap[2])
        }
        TournamentPodiumSlot {
            TournamentPodiumEntry(position = LeaderboardHelpers.POS_GOLD, row = first, prize = prizeMap[1])
        }
        TournamentPodiumSlot {
            TournamentPodiumEntry(position = LeaderboardHelpers.POS_BRONZE, row = third, prize = prizeMap[3])
        }
    }
}

@Composable
private fun RowScope.TournamentPodiumSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        content()
    }
}

@Composable
private fun TournamentPodiumEntry(
    position: Int,
    row: LeaderboardRow?,
    prize: Int?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(max = 93.dp),
    ) {
        TournamentTrophy(position = position)
        (row?.prize ?: prize)?.let { amount ->
            LeaderboardReward(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 93.dp),
                rewardCurrency = RewardCurrency.BTC,
                rewardCurrencyCode = "INR",
                reward = amount.toDouble(),
                isBackgroundVisible = true,
                backgroundColor = YralColors.Yellow100,
            )
        }
        Text(
            text = formatUsername(row),
            style = LocalAppTopography.current.regSemiBold,
            color = YralColors.Neutral950,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 93.dp),
        )
    }
}

@Composable
private fun TournamentTrophy(position: Int) {
    val trophy =
        when (position) {
            LeaderboardHelpers.POS_GOLD -> LeaderboardRes.drawable.golden_trophy
            LeaderboardHelpers.POS_SILVER -> LeaderboardRes.drawable.silver_trophy
            LeaderboardHelpers.POS_BRONZE -> LeaderboardRes.drawable.bronze_trophy
            else -> LeaderboardRes.drawable.bronze_trophy
        }
    val width = LeaderboardHelpers.getTrophyImageWidth(position)
    val height = LeaderboardHelpers.getTrophyImageHeight(position)
    val offset = LeaderboardHelpers.getTrophyImageOffset(position = position, isProfileImageVisible = false)
    Box(
        modifier = Modifier.height(height + offset),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(trophy),
            contentDescription = null,
            modifier =
                Modifier
                    .width(width)
                    .height(height)
                    .offset(y = offset),
            contentScale = ContentScale.Crop,
        )
    }
}

@Suppress("MagicNumber", "LongMethod")
@Composable
private fun TournamentLeaderboardTableHeader(isTrophyVisible: Boolean) {
    val headerWeights = TOURNAMENT_LEADERBOARD_HEADER_WEIGHTS

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
            text = stringResource(Res.string.tournament_leaderboard_rank),
            modifier = Modifier.weight(headerWeights[0]),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.tournament_leaderboard_player),
            modifier =
                Modifier
                    .weight(headerWeights[1])
                    .padding(start = 6.dp),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.tournament_leaderboard_diamonds),
            modifier = Modifier.weight(headerWeights[2]),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.Neutral500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.5.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(headerWeights[3]),
        ) {
            Text(
                text = stringResource(Res.string.tournament_leaderboard_rewards),
                style = LocalAppTopography.current.regRegular,
                textAlign = TextAlign.Center,
                color = YralColors.NeutralTextSecondary,
            )
        }
    }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
private fun TournamentLeaderboardRow(
    row: LeaderboardRow,
    isCurrentUser: Boolean,
    fallbackPrize: Int?,
    profileImageUrl: String,
    isPro: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        when {
            isCurrentUser -> Color(0xFFFF78C1)
            row.position == LeaderboardHelpers.POS_GOLD -> Color(0xFFBF760B)
            row.position == LeaderboardHelpers.POS_SILVER -> Color(0xFF2F2F30)
            row.position == LeaderboardHelpers.POS_BRONZE -> Color(0xFF6D4C35)
            else -> Color.Transparent
        }
    val containerColor =
        if (isCurrentUser) {
            Color(0xFFA00157)
        } else {
            YralColors.Neutral900
        }
    Surface(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border =
            if (borderColor == Color.Transparent) {
                null
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        ) {
            val rowWeights = LeaderboardUiConstants.TOURNAMENT_LEADERBOARD_ROW_WEIGHTS
            Box(
                modifier = Modifier.weight(rowWeights[0]),
                contentAlignment = Alignment.CenterStart,
            ) {
                TournamentPosition(position = row.position, decorateCurrentUser = isCurrentUser)
            }
            Row(
                modifier = Modifier.weight(rowWeights[1]),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            ) {
                TournamentAvatar(
                    position = row.position,
                    principalId = row.principalId,
                    profileImageUrl = profileImageUrl,
                    isPro = isPro,
                )
                TournamentUsername(
                    position = row.position,
                    username = formatUsername(row),
                    isCurrentUser = isCurrentUser,
                )
            }
            Box(
                modifier = Modifier.weight(rowWeights[2]),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = formatAbbreviation(row.diamonds.toLong()),
                    style = LocalAppTopography.current.baseBold,
                    color = YralColors.Neutral50,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RewardsCell(
                amount = row.prize ?: fallbackPrize,
                modifier = Modifier.weight(rowWeights[3]),
            )
        }
    }
}

@Composable
private fun TournamentPosition(
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
private fun TournamentAvatar(
    position: Int,
    principalId: String,
    profileImageUrl: String,
    isPro: Boolean = false,
) {
    val imageUrl = profileImageUrl.ifBlank { propicFromPrincipal(principalId) }
    Box(modifier = Modifier.wrapContentSize()) {
        ProfileImageView(
            imageUrl = imageUrl,
            size = LeaderboardHelpers.PROFILE_IMAGE_SIZE.dp,
            applyFrame = isPro,
        )
        if (!isPro) {
            val ring = LeaderboardHelpers.getProfileImageRing(position)
            if (ring != null) {
                Image(
                    painter = painterResource(ring),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }
        }
    }
}

@Composable
private fun TournamentUsername(
    position: Int,
    username: String,
    isCurrentUser: Boolean,
) {
    val decoration = LeaderboardHelpers.getTextDecoration(position)
    val textStyle = LocalAppTopography.current.baseMedium
    if (decoration != null && !isCurrentUser) {
        YralMaskedVectorTextV2(
            text = username,
            drawableRes = decoration,
            textStyle = textStyle,
            maxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(84.dp),
        )
    } else {
        Text(
            text = if (isCurrentUser) stringResource(LeaderboardRes.string.you) else username,
            style = textStyle,
            color = YralColors.NeutralTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(84.dp),
        )
    }
}

@Composable
private fun RewardsCell(
    amount: Int?,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        if (amount == null) {
            Text(
                text = "₹0",
                style = LocalAppTopography.current.baseBold,
                color = YralColors.Neutral50,
            )
        } else {
            Text(
                text = "₹$amount",
                style = LocalAppTopography.current.baseBold,
                color = YralColors.Neutral50,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Image(
            painter = painterResource(Res.drawable.bitcoin),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun formatUsername(row: LeaderboardRow?): String {
    if (row == null) return "-"
    val resolved = resolveUsername(row.username, row.principalId) ?: row.principalId
    return if (resolved.startsWith("@")) resolved else "@$resolved"
}

private fun profileImageUrlFor(
    principalId: String,
    profileDetailsMap: Map<String, UserProfileDetails>,
): String =
    profileDetailsMap[principalId]?.profilePictureUrl?.takeIf { it.isNotBlank() }
        ?: propicFromPrincipal(principalId)

private fun isProFor(
    principalId: String,
    profileDetailsMap: Map<String, UserProfileDetails>,
): Boolean = profileDetailsMap[principalId]?.subscriptionPlan is SubscriptionPlan.Pro
