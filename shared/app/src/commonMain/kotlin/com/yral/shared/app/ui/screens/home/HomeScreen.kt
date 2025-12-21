package com.yral.shared.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.WalletFeatureFlags
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.app.ui.screens.feed.FeedScaffoldScreen
import com.yral.shared.app.ui.screens.home.nav.HomeComponent
import com.yral.shared.app.ui.screens.home.nav.HomeComponent.SlotChild
import com.yral.shared.app.ui.screens.profile.ProfileScreen
import com.yral.shared.app.ui.screens.uploadVideo.UploadVideoRootScreen
import com.yral.shared.core.session.SessionKey
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.session.getKey
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.account.ui.AccountScreen
import com.yral.shared.features.account.ui.AlertsPermissionController
import com.yral.shared.features.account.ui.rememberAlertsPermissionController
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.game.viewmodel.GameViewModel
import com.yral.shared.features.leaderboard.ui.LeaderboardScreen
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.features.tournament.ui.TournamentScreen
import com.yral.shared.features.tournament.viewmodel.TournamentViewModel
import com.yral.shared.features.wallet.ui.WalletScreen
import com.yral.shared.features.wallet.ui.btcRewards.VideoViewsRewardsBottomSheet
import com.yral.shared.libs.designsystem.component.YralFeedback
import com.yral.shared.libs.designsystem.component.popPressedSoundUri
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.home_nav_selected
import yral_mobile.shared.app.generated.resources.home_nav_unselected
import yral_mobile.shared.app.generated.resources.leaderboard_nav_selected
import yral_mobile.shared.app.generated.resources.leaderboard_nav_unselected
import yral_mobile.shared.app.generated.resources.new_
import yral_mobile.shared.app.generated.resources.profile_nav_selected
import yral_mobile.shared.app.generated.resources.profile_nav_unselected
import yral_mobile.shared.app.generated.resources.upload_video_nav_selected
import yral_mobile.shared.app.generated.resources.upload_video_nav_unselected
import yral_mobile.shared.app.generated.resources.wallet_nav
import yral_mobile.shared.app.generated.resources.wallet_nav_unselected
import yral_mobile.shared.libs.designsystem.generated.resources.account_nav
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
internal fun HomeScreen(
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
                    is HomeComponent.Child.Tournament -> HomeTab.TOURNAMENT
                    is HomeComponent.Child.Profile -> HomeTab.PROFILE
                    is HomeComponent.Child.UploadVideo -> HomeTab.UPLOAD_VIDEO
                    is HomeComponent.Child.Wallet -> HomeTab.WALLET
                }
            val updateCurrentTab: (tab: HomeTab) -> Unit = { tab ->
                when (tab) {
                    HomeTab.ACCOUNT -> component.onAccountTabClick()
                    HomeTab.HOME -> component.onFeedTabClick()
                    HomeTab.LEADER_BOARD -> component.onLeaderboardTabClick()
                    HomeTab.TOURNAMENT -> component.onTournamentTabClick()
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
            sessionState = sessionState,
            updateProfileVideosCount = updateProfileVideosCount,
        )
        SlotContent(component)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotContent(component: HomeComponent) {
    val slot by component.slot.subscribeAsState()
    slot.child?.instance?.also { slotChild ->
        when (slotChild) {
            is SlotChild.VideoViewsRewardsBottomSheet ->
                VideoViewsRewardsBottomSheet(
                    component = slotChild.component,
                    data = slotChild.data,
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
private fun HomeScreenContent(
    component: HomeComponent,
    modifier: Modifier,
    innerPadding: PaddingValues,
    sessionState: SessionState,
    updateProfileVideosCount: (count: Int) -> Unit,
) {
    val sessionKey = sessionState.getKey()
    val canisterData = sessionState.getCanisterData()
    val feedViewModel = koinViewModel<FeedViewModel>(key = "feed-$sessionKey")
    val gameViewModel = koinViewModel<GameViewModel>(key = "game-$sessionKey")
    val profileViewModel =
        koinViewModel<ProfileViewModel>(key = "profile-$sessionKey") {
            parametersOf(canisterData)
        }
    val accountViewModel = koinViewModel<AccountsViewModel>(key = "account-$sessionKey")
    val leaderBoardViewModel = koinViewModel<LeaderBoardViewModel>(key = "leaderboard-$sessionKey")
    val tournamentViewModel = koinViewModel<TournamentViewModel>(key = "tournament-$sessionKey")

    val profileVideos = getProfileVideos(profileViewModel, sessionKey, updateProfileVideosCount)

    val alertsPermissionController = rememberAlertsPermissionController(accountViewModel)
    NotificationPermissionObserver(alertsPermissionController, accountViewModel)

    Children(
        stack = component.stack,
        modifier = modifier,
    ) {
        when (val child = it.instance) {
            is HomeComponent.Child.Feed ->
                FeedScaffoldScreen(
                    component = child.component,
                    feedViewModel = feedViewModel,
                    gameViewModel = gameViewModel,
                    leaderBoardViewModel = leaderBoardViewModel,
                )

            is HomeComponent.Child.Account -> {
                AccountScreen(
                    component = child.component,
                    viewModel = accountViewModel,
                    onAlertsToggleRequest = alertsPermissionController.toggle,
                )
            }

            is HomeComponent.Child.Leaderboard ->
                LeaderboardScreen(
                    component = child.component,
                    leaderBoardViewModel = leaderBoardViewModel,
                )

            is HomeComponent.Child.Tournament ->
                TournamentScreen(
                    viewModel = tournamentViewModel,
                )

            is HomeComponent.Child.UploadVideo ->
                UploadVideoRootScreen(
                    component = child.component,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                )

            is HomeComponent.Child.Profile ->
                profileVideos?.let {
                    ProfileScreen(
                        component = child.component,
                        profileViewModel = profileViewModel,
                        accountsViewModel = accountViewModel,
                        profileVideos = profileVideos,
                        onAlertsToggleRequest = alertsPermissionController.toggle,
                    )
                }

            is HomeComponent.Child.Wallet ->
                WalletScreen(
                    component = child.component,
                )
        }
        LoginIfRequired(
            currentChild = it.instance,
            component = component,
        )
    }
}

@Composable
private fun NotificationPermissionObserver(
    alertsPermissionController: AlertsPermissionController,
    accountViewModel: AccountsViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner, alertsPermissionController) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    scope.launch {
                        val actual =
                            runCatching { alertsPermissionController.currentStatus() }
                                .getOrElse { accountViewModel.state.value.alertsEnabled }
                        if (actual != accountViewModel.state.value.alertsEnabled) {
                            accountViewModel.onAlertsToggleChanged(actual)
                        }
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                    HomeTab.TOURNAMENT -> false
                    else -> true
                }
            }
    val insetHeightPx = NavigationBarDefaults.windowInsets.getBottom(LocalDensity.current)
    val insetHeightDp = with(LocalDensity.current) { insetHeightPx.toDp() }
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            Modifier
                .height(67.dp + insetHeightDp),
    ) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                selected = currentTab == tab,
                onClick = {
                    playSound = true
                    updateCurrentTab(tab)
                    bottomNavigationClicked(tab.categoryName)
                },
                icon = {
                    NavBarIcon(
                        isSelected = currentTab == tab,
                        tab = tab,
                    )
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
            soundUri = popPressedSoundUri(),
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
                    text = stringResource(Res.string.new_),
                    style = LocalAppTopography.current.xsBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // Use weight to push icon to center of the available space
            Spacer(modifier = Modifier.weight(1f))
        }

        val icon =
            if (isSelected) {
                tab.icon
            } else {
                tab.unSelectedIcon
            }
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(icon),
            contentDescription = tab.title,
            tint = Color.White,
        )
    }
}

private enum class HomeTab(
    val title: String,
    val categoryName: CategoryName,
    val icon: DrawableResource,
    val unSelectedIcon: DrawableResource,
    val isNew: Boolean = false,
) {
    HOME(
        title = "Home",
        categoryName = CategoryName.HOME,
        icon = Res.drawable.home_nav_selected,
        unSelectedIcon = Res.drawable.home_nav_unselected,
    ),
    LEADER_BOARD(
        title = "LeaderBoard",
        categoryName = CategoryName.LEADERBOARD,
        icon = Res.drawable.leaderboard_nav_selected,
        unSelectedIcon = Res.drawable.leaderboard_nav_unselected,
    ),
    TOURNAMENT(
        title = "Tournament",
        categoryName = CategoryName.TOURNAMENTS,
        icon = Res.drawable.leaderboard_nav_selected,
        unSelectedIcon = Res.drawable.leaderboard_nav_unselected,
        isNew = true,
    ),
    UPLOAD_VIDEO(
        title = "UploadVideo",
        categoryName = CategoryName.UPLOAD_VIDEO,
        icon = Res.drawable.upload_video_nav_selected,
        unSelectedIcon = Res.drawable.upload_video_nav_unselected,
    ),
    WALLET(
        title = "Wallet",
        categoryName = CategoryName.WALLET,
        icon = Res.drawable.wallet_nav,
        unSelectedIcon = Res.drawable.wallet_nav_unselected,
        isNew = true,
    ),
    PROFILE(
        title = "Profile",
        categoryName = CategoryName.PROFILE,
        icon = Res.drawable.profile_nav_selected,
        unSelectedIcon = Res.drawable.profile_nav_unselected,
    ),
    ACCOUNT(
        title = "Account",
        categoryName = CategoryName.MENU,
        icon = DesignRes.drawable.account_nav,
        unSelectedIcon = DesignRes.drawable.account_nav,
    ),
}

private fun SessionState.getCanisterData(): CanisterData =
    when (this) {
        SessionState.Initial,
        SessionState.Loading,
        ->
            CanisterData(
                canisterId = "",
                userPrincipalId = "",
                profilePic = "",
                username = "",
                isCreatedFromServiceCanister = true,
            )
        is SessionState.SignedIn ->
            CanisterData(
                canisterId = session.canisterId ?: "",
                userPrincipalId = session.userPrincipal ?: "",
                profilePic = session.profilePic ?: "",
                username = session.username,
                isCreatedFromServiceCanister = session.isCreatedFromServiceCanister,
            )
    }

@Suppress("LongMethod")
@Composable
private fun LoginIfRequired(
    currentChild: HomeComponent.Child,
    component: HomeComponent,
) {
    val homeState by component.homeViewModel.state.collectAsStateWithLifecycle()
    val dismissSheet =
        remember {
            {
                component.homeViewModel.showSignupPrompt(false, null)
                component.hideLoginBottomSheetIfVisible()
            }
        }
    LaunchedEffect(homeState.showSignupPrompt, homeState.isSocialSignedIn) {
        if (homeState.isSocialSignedIn || !homeState.showSignupPrompt) return@LaunchedEffect
        when (currentChild) {
            is HomeComponent.Child.Feed -> {
                component.showLoginBottomSheet(
                    pageName = SignupPageName.HOME,
                    loginBottomSheetType = LoginBottomSheetType.FEED,
                    onDismissRequest = dismissSheet,
                    onLoginSuccess = dismissSheet,
                )
            }
            is HomeComponent.Child.UploadVideo -> {
                component.showLoginBottomSheet(
                    pageName = homeState.pageName ?: SignupPageName.UPLOAD_VIDEO,
                    loginBottomSheetType = LoginBottomSheetType.UPLOAD_AI_VIDEO,
                    onDismissRequest = dismissSheet,
                    onLoginSuccess = dismissSheet,
                )
            }
            is HomeComponent.Child.Profile -> {
                component.showLoginBottomSheet(
                    pageName = homeState.pageName ?: SignupPageName.MENU,
                    loginBottomSheetType = LoginBottomSheetType.DEFAULT,
                    onDismissRequest = dismissSheet,
                    onLoginSuccess = {
                        dismissSheet()
                        currentChild.component.openProfile()
                    },
                )
            }
            else -> Unit
        }
    }
    LaunchedEffect(currentChild, homeState.hasShownSignupPrompt, homeState.isSocialSignedIn) {
        if (homeState.isSocialSignedIn) return@LaunchedEffect
        when (currentChild) {
            is HomeComponent.Child.Leaderboard -> {
                if (homeState.hasShownSignupPrompt[SignupPageName.LEADERBOARD] != true) {
                    component.showLoginBottomSheet(
                        pageName = SignupPageName.LEADERBOARD,
                        loginBottomSheetType = LoginBottomSheetType.DEFAULT,
                        onDismissRequest = {
                            dismissSheet()
                            component.homeViewModel.onSignupPromptShown(SignupPageName.LEADERBOARD)
                        },
                        onLoginSuccess = {
                            dismissSheet()
                            component.homeViewModel.onSignupPromptShown(SignupPageName.LEADERBOARD)
                        },
                    )
                }
            }
            else -> Unit
        }
    }
}
