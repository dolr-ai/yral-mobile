package com.yral.android.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.android.R
import com.yral.android.ui.screens.account.AccountScreen
import com.yral.android.ui.screens.alertsrequest.AlertsRequestBottomSheet
import com.yral.android.ui.screens.feed.FeedScreen
import com.yral.android.ui.screens.home.nav.HomeComponent
import com.yral.android.ui.screens.home.nav.HomeComponent.SlotChild
import com.yral.android.ui.screens.profile.ProfileScreen
import com.yral.android.ui.screens.uploadVideo.UploadVideoRootScreen
import com.yral.android.ui.screens.wallet.WalletScreen
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.WalletFeatureFlags
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.core.session.SessionKey
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.session.getKey
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.game.viewmodel.GameViewModel
import com.yral.shared.features.leaderboard.ui.leaderboard.LeaderboardScreen
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.libs.designsystem.component.YralFeedback
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    component: HomeComponent,
    updateProfileVideosCount: (count: Int) -> Unit,
    bottomNavigationAnalytics: (categoryName: CategoryName) -> Unit,
    sessionState: SessionState,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val stack by component.stack.subscribeAsState()
            val activeComponent = stack.active.instance
            val currentTab =
                when (activeComponent) {
                    is HomeComponent.Child.Account -> HomeTab.ACCOUNT
                    is HomeComponent.Child.Feed -> HomeTab.HOME
                    is HomeComponent.Child.Leaderboard -> HomeTab.LEADER_BOARD
                    is HomeComponent.Child.Profile -> HomeTab.PROFILE
                    is HomeComponent.Child.UploadVideo -> HomeTab.UPLOAD_VIDEO
                    is HomeComponent.Child.Wallet -> HomeTab.WALLET
                }
            val updateCurrentTab: (tab: HomeTab) -> Unit = { tab ->
                when (tab) {
                    HomeTab.ACCOUNT -> component.onAccountTabClick()
                    HomeTab.HOME -> component.onFeedTabClick()
                    HomeTab.LEADER_BOARD -> component.onLeaderboardTabClick()
                    HomeTab.PROFILE -> component.onProfileTabClick()
                    HomeTab.UPLOAD_VIDEO -> component.onUploadVideoTabClick()
                    HomeTab.WALLET -> component.onWalletTabClick()
                }
            }
            HomeNavigationBar(
                currentTab = currentTab,
                updateCurrentTab = updateCurrentTab,
                bottomNavigationClicked = bottomNavigationAnalytics,
            )
        },
    ) { innerPadding ->
        HomeScreenContent(
            component = component,
            modifier =
                Modifier
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            innerPadding = innerPadding,
            sessionKey = sessionState.getKey(),
            updateProfileVideosCount = updateProfileVideosCount,
        )
        SlotContent(component)
    }
}

@Composable
private fun SlotContent(component: HomeComponent) {
    val slot by component.slot.subscribeAsState()
    slot.child?.instance?.also { slotChild ->
        when (slotChild) {
            is SlotChild.AlertsRequestBottomSheet ->
                AlertsRequestBottomSheet(
                    component = slotChild.component,
                )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun HomeScreenContent(
    component: HomeComponent,
    modifier: Modifier,
    innerPadding: PaddingValues,
    sessionKey: String,
    updateProfileVideosCount: (count: Int) -> Unit,
) {
    val feedViewModel = koinViewModel<FeedViewModel>(key = "feed-$sessionKey")
    val gameViewModel = koinViewModel<GameViewModel>(key = "game-$sessionKey")
    val profileViewModel = koinViewModel<ProfileViewModel>(key = "profile-$sessionKey")
    val accountViewModel = koinViewModel<AccountsViewModel>(key = "account-$sessionKey")
    val profileVideos = getProfileVideos(profileViewModel, sessionKey, updateProfileVideosCount)
    Children(
        stack = component.stack,
        modifier = modifier,
    ) {
        when (val child = it.instance) {
            is HomeComponent.Child.Feed ->
                FeedScreen(
                    component = child.component,
                    viewModel = feedViewModel,
                    gameViewModel = gameViewModel,
                )

            is HomeComponent.Child.Account ->
                AccountScreen(
                    component = child.component,
                    viewModel = accountViewModel,
                )

            is HomeComponent.Child.Leaderboard ->
                LeaderboardScreen(
                    component = child.component,
                )

            is HomeComponent.Child.UploadVideo -> {
                UploadVideoRootScreen(
                    component = child.component,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                )
            }

            is HomeComponent.Child.Profile -> {
                profileVideos?.let {
                    ProfileScreen(
                        component = child.component,
                        profileViewModel = profileViewModel,
                        accountsViewModel = accountViewModel,
                        profileVideos = profileVideos,
                    )
                }
            }

            is HomeComponent.Child.Wallet -> {
                WalletScreen(
                    component = child.component,
                )
            }
        }
    }
}

@Composable
private fun getProfileVideos(
    profileViewModel: ProfileViewModel,
    sessionKey: String,
    updateProfileVideosCount: (count: Int) -> Unit,
): LazyPagingItems<FeedDetails>? {
    if (sessionKey != SessionKey.INITIAL.name) {
        val profileVideos =
            profileViewModel
                .profileVideos
                .collectAsLazyPagingItems()
        LaunchedEffect(profileVideos.loadState, profileVideos.itemCount) {
            if (profileVideos.loadState.refresh is LoadState.NotLoading) {
                updateProfileVideosCount(profileVideos.itemCount)
            }
        }
        return profileVideos
    } else {
        return null
    }
}

@Suppress("LongMethod")
@Composable
private fun HomeNavigationBar(
    currentTab: HomeTab,
    updateCurrentTab: (tab: HomeTab) -> Unit,
    bottomNavigationClicked: (categoryName: CategoryName) -> Unit,
) {
    var playSound by remember { mutableStateOf(false) }
    val flagManager = koinInject<FeatureFlagManager>()
    val isWalletEnabled = flagManager.isEnabled(WalletFeatureFlags.Wallet.Enabled)
    val tabs =
        HomeTab.entries
            .filter {
                when (it) {
                    HomeTab.ACCOUNT -> !isWalletEnabled
                    HomeTab.WALLET -> isWalletEnabled
                    else -> true
                }
            }
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
        tabs.forEachIndexed { index, tab ->
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
                selected = currentTab == tab,
                onClick = {
                    playSound = true
                    updateCurrentTab(tab)
                    bottomNavigationClicked(tab.categoryName)
                },
                icon = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = alignment,
                    ) {
                        NavBarIcon(
                            isSelected = currentTab == tab,
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
    if (playSound) {
        YralFeedback(
            sound = R.raw.pop_pressed,
            withHapticFeedback = true,
        ) { playSound = false }
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

private enum class HomeTab(
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
    ),
    UPLOAD_VIDEO(
        title = "UploadVideo",
        categoryName = CategoryName.UPLOAD_VIDEO,
        icon = R.drawable.upload_video_nav_selected,
        unSelectedIcon = R.drawable.upload_video_nav_unselected,
    ),
    WALLET(
        title = "Wallet",
        categoryName = CategoryName.WALLET,
        icon = R.drawable.wallet_nav,
        unSelectedIcon = R.drawable.wallet_nav_unselected,
        isNew = true,
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
