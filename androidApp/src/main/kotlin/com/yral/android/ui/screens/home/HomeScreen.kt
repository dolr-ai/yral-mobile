package com.yral.android.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.yral.android.R
import com.yral.android.ui.components.hashtagInput.keyboardHeightAsState
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.account.AccountScreen
import com.yral.android.ui.screens.feed.FeedScreen
import com.yral.android.ui.screens.leaderboard.LeaderboardScreen
import com.yral.android.ui.screens.profile.ProfileScreen
import com.yral.android.ui.screens.uploadVideo.UploadVideoScreen
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.session.getKey
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.koin.koinInstance
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    currentTab: String,
    updateCurrentTab: (tab: String) -> Unit,
    bottomNavigationAnalytics: (categoryName: CategoryName) -> Unit,
    sessionState: SessionState,
) {
    val backHandlerEnabled by remember(currentTab) {
        mutableStateOf(currentTab != HomeTab.HOME.title)
    }
    BackHandler(
        enabled = backHandlerEnabled,
        onBack = { updateCurrentTab(HomeTab.HOME.title) },
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            HomeNavigationBar(
                currentTab = currentTab,
                updateCurrentTab = updateCurrentTab,
                bottomNavigationClicked = bottomNavigationAnalytics,
            )
        },
    ) { innerPadding ->
        HomeScreenContent(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            innerPadding = innerPadding,
            sessionKey = sessionState.getKey(),
            currentTab = currentTab,
            updateCurrentTab = { updateCurrentTab(it.title) },
        )
    }
}

@Composable
private fun HomeScreenContent(
    modifier: Modifier,
    innerPadding: PaddingValues,
    sessionKey: String,
    currentTab: String,
    updateCurrentTab: (tab: HomeTab) -> Unit,
) {
    val feedViewModel = koinViewModel<FeedViewModel>(key = "feed-$sessionKey")
    val profileViewModel = koinViewModel<ProfileViewModel>(key = "profile-$sessionKey")
    val profileVideos = profileViewModel.profileVideos.collectAsLazyPagingItems()
    val accountViewModel = koinViewModel<AccountsViewModel>(key = "account-$sessionKey")
    when (currentTab) {
        HomeTab.HOME.title ->
            FeedScreen(
                modifier = modifier,
                viewModel = feedViewModel,
            )

        HomeTab.ACCOUNT.title ->
            AccountScreen(
                modifier = modifier,
                viewModel = accountViewModel,
            )

        HomeTab.LEADER_BOARD.title ->
            LeaderboardScreen(
                modifier = modifier,
                viewModel = koinInstance.get(),
            )

        HomeTab.UPLOAD_VIDEO.title -> {
            val keyboardHeight by keyboardHeightAsState()
            val bottomPadding by remember(keyboardHeight) {
                mutableStateOf(
                    if (keyboardHeight > 0) {
                        0.dp
                    } else {
                        innerPadding.calculateBottomPadding()
                    },
                )
            }
            UploadVideoScreen(
                modifier =
                    Modifier
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = bottomPadding,
                        ).background(MaterialTheme.colorScheme.primaryContainer),
                goToHome = { updateCurrentTab(HomeTab.HOME) },
            )
        }

        HomeTab.PROFILE.title -> {
            ProfileScreen(
                modifier = modifier,
                uploadVideo = { updateCurrentTab(HomeTab.UPLOAD_VIDEO) },
                viewModel = profileViewModel,
                profileVideos = profileVideos,
            )
        }
    }
}

@Composable
private fun HomeNavigationBar(
    currentTab: String,
    updateCurrentTab: (tab: String) -> Unit,
    bottomNavigationClicked: (categoryName: CategoryName) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(67.dp)
                .padding(start = 16.dp, end = 16.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        HomeTab.entries.forEachIndexed { index, tab ->
            val alignment =
                remember {
                    when (index) {
                        0 -> Alignment.CenterStart
                        HomeTab.entries.size - 1 -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }
                }
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                selected = currentTab == tab.title,
                onClick = {
                    updateCurrentTab(tab.title)
                    bottomNavigationClicked(tab.categoryName)
                },
                icon = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = alignment,
                    ) {
                        NavBarIcon(
                            isSelected = currentTab == tab.title,
                            tab = tab,
                        )
                    }
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        indicatorColor = Color.Transparent,
                    ),
            )
        }
    }
}

@Composable
private fun NavBarIcon(
    isSelected: Boolean,
    tab: HomeTab,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.height(67.dp),
    ) {
        // Show indicator line on top when selected
        Box(
            modifier =
                Modifier
                    .width(30.dp)
                    .height(2.dp)
                    .background(
                        color =
                            if (isSelected) {
                                YralColors.Pink300
                            } else {
                                Color.Transparent
                            },
                        shape = RoundedCornerShape(100.dp),
                    ),
        )
        NewTaggedColumn(
            tab = tab,
            isSelected = isSelected,
        )
    }
}

@Composable
private fun NewTaggedColumn(
    tab: HomeTab,
    isSelected: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(10.dp),
    ) {
        if (tab.isNew) {
            Row(
                modifier =
                    Modifier
                        .padding(top = 5.dp)
                        .background(
                            color = YralColors.Pink300,
                            shape = RoundedCornerShape(size = 12.dp),
                        ).padding(start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.new_),
                    style = LocalAppTopography.current.xsBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // Use weight to push icon to center of the available space
            Spacer(modifier = Modifier.weight(1f))
        }

        Icon(
            modifier = Modifier.size(32.dp),
            painter =
                if (isSelected) {
                    painterResource(tab.icon)
                } else {
                    painterResource(tab.unSelectedIcon)
                },
            contentDescription = tab.title,
            tint = Color.White,
        )
    }
}

enum class HomeTab(
    val title: String,
    val categoryName: CategoryName,
    val icon: Int,
    val unSelectedIcon: Int,
    val isNew: Boolean = false,
) {
    HOME(
        title = "Home",
        categoryName = CategoryName.HOME,
        icon = R.drawable.home_nav_selected,
        unSelectedIcon = R.drawable.home_nav_unselected,
    ),
    LEADER_BOARD(
        title = "LeaderBoard",
        categoryName = CategoryName.LEADERBOARD,
        icon = R.drawable.leaderboard_nav_selected,
        unSelectedIcon = R.drawable.leaderboard_nav_unselected,
        isNew = true,
    ),
    UPLOAD_VIDEO(
        title = "UploadVideo",
        categoryName = CategoryName.UPLOAD_VIDEO,
        icon = R.drawable.upload_video_nav_selected,
        unSelectedIcon = R.drawable.upload_video_nav_unselected,
    ),
    PROFILE(
        title = "Profile",
        categoryName = CategoryName.PROFILE,
        icon = R.drawable.profile_nav_selected,
        unSelectedIcon = R.drawable.profile_nav_unselected,
    ),
    ACCOUNT(
        title = "Account",
        categoryName = CategoryName.MENU,
        icon = R.drawable.account_nav,
        unSelectedIcon = R.drawable.account_nav,
    ),
}
