package com.yral.android.ui.screens.leaderboard.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.screens.leaderboard.LeaderboardRow
import com.yral.android.ui.screens.leaderboard.LeaderboardTableHeader
import com.yral.android.ui.screens.leaderboard.main.LeaderboardConfetti
import com.yral.shared.features.leaderboard.viewmodel.LeaderboardHistoryViewModel
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max
import kotlin.math.min

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun LeaderboardDetailsScreen(
    component: LeaderboardDetailsComponent,
    viewModel: LeaderboardHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showConfetti by remember(state.selectedIndex, state.history) { mutableStateOf(viewModel.isCurrentUserInTop()) }
    LaunchedEffect(Unit) { viewModel.fetchHistory() }
    val listState = rememberLazyListState()
    var pageLoadedReported by remember(state.selectedIndex) { mutableStateOf(false) }
    LaunchedEffect(state.isLoading, state.selectedIndex) {
        if (state.isLoading) pageLoadedReported = false
    }
    LaunchedEffect(listState, state.isLoading, state.error, state.selectedIndex) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { itemsInfo ->
                if (!state.isLoading && state.error == null && !pageLoadedReported) {
                    val viewportStart = listState.layoutInfo.viewportStartOffset
                    val viewportEnd = listState.layoutInfo.viewportEndOffset
                    val visibleRowCount =
                        itemsInfo.count { info ->
                            if (info.contentType != "leaderboardRow") return@count false
                            val itemStart = info.offset
                            val itemEnd = info.offset + info.size
                            val visible = min(itemEnd, viewportEnd) - max(itemStart, viewportStart)
                            visible > 0 && visible * 2 > info.size
                        }
                    if (visibleRowCount > 0) {
                        viewModel.reportLeaderboardDaySelected(visibleRowCount)
                        pageLoadedReported = true
                    }
                }
            }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            state = listState,
        ) {
            stickyHeader {
                Column(Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
                    Header { component.onBack() }
                    Spacer(Modifier.height(22.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .background(YralColors.Neutral800)
                                .fillMaxWidth()
                                .height(38.dp)
                                .padding(start = 16.dp),
                    ) {
                        val dates = state.history.map { it.date }
                        itemsIndexed(dates) { index, date ->
                            val index = dates.indexOf(date)
                            DateChip(
                                date = date,
                                isSelected = index == state.selectedIndex,
                                select = { viewModel.select(index) },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LeaderboardTableHeader(false)
                }
            }
            if (!state.isLoading && state.error == null && state.history.isNotEmpty()) {
                val selected = state.history.getOrNull(state.selectedIndex)
                selected?.let { day ->
                    day.userRow?.let { userRow ->
                        item(contentType = "leaderboardRow") {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                LeaderboardRow(
                                    position = userRow.position,
                                    userPrincipalId = userRow.userPrincipalId,
                                    profileImageUrl = userRow.profileImage,
                                    wins = userRow.wins,
                                    isCurrentUser = true,
                                    decorateCurrentUser = true,
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                    items(items = day.topRows, contentType = { "leaderboardRow" }) { row ->
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            LeaderboardRow(
                                position = row.position,
                                userPrincipalId = row.userPrincipalId,
                                profileImageUrl = row.profileImage,
                                wins = row.wins,
                                isCurrentUser = viewModel.isCurrentUser(row.userPrincipalId),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    item { Spacer(Modifier.height(68.dp)) }
                }
            }
            if (!state.isLoading && state.error != null) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                YralLoader()
            }
        }
        LeaderboardConfetti(showConfetti) { showConfetti = false }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .height(54.dp)
                .padding(vertical = 12.dp, horizontal = 12.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.arrow_left),
            contentDescription = "image description",
            contentScale = ContentScale.None,
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable { onBack() },
        )
        Text(
            text = stringResource(R.string.weekly_wins),
            style = LocalAppTopography.current.xlBold,
            modifier = Modifier.fillMaxWidth(),
            color = YralColors.NeutralIconsActive,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DateChip(
    date: String,
    isSelected: Boolean,
    select: () -> Unit,
) {
    val chipBackground =
        if (isSelected) {
            YralColors.Neutral50
        } else {
            Color.Transparent
        }
    val chipTextColor =
        if (isSelected) {
            YralColors.Yellow400
        } else {
            YralColors.Yellow300
        }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .height(30.dp)
                .background(color = chipBackground, shape = RoundedCornerShape(size = 32.dp))
                .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                .clickable { select() },
    ) {
        Text(
            text = date,
            style = LocalAppTopography.current.mdBold,
            color = chipTextColor,
        )
    }
}
