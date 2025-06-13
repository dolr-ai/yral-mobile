package com.yral.android.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.home.account.AccountScreen
import com.yral.android.ui.screens.home.feed.FeedScreen
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.koin.koinInstance

@Composable
fun HomeScreen(
    createFeedViewModel: () -> FeedViewModel,
    currentTab: String,
    updateCurrentTab: (tab: String) -> Unit,
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
            )
        },
    ) { innerPadding ->
        when (currentTab) {
            HomeTab.HOME.title ->
                FeedScreen(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    viewModel = createFeedViewModel(),
                )

            HomeTab.ACCOUNT.title ->
                AccountScreen(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    viewModel = koinInstance.get(),
                )
        }
    }
}

@Composable
private fun HomeNavigationBar(
    currentTab: String,
    updateCurrentTab: (tab: String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            Modifier
                .navigationBarsPadding()
                .height(67.dp)
                .padding(start = 36.dp, end = 36.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        HomeTab.entries.forEach { tab ->
            if (tab.isGhost) {
                // Create invisible spacer item
                NavigationBarItem(
                    selected = false,
                    onClick = { /* No-op */ },
                    icon = { Box {} },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Transparent,
                            unselectedIconColor = Color.Transparent,
                            indicatorColor = Color.Transparent,
                        ),
                )
            } else {
                NavigationBarItem(
                    selected = currentTab == tab.title,
                    onClick = { updateCurrentTab(tab.title) },
                    icon = {
                        NavBarIcon(
                            isSelected = currentTab == tab.title,
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
    }
}

@Composable
private fun NavBarIcon(
    isSelected: Boolean,
    tab: HomeTab,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
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

        // Use weight to push icon to center of the available space
        Spacer(modifier = Modifier.weight(1f))

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

        // Balance space at bottom
        Spacer(modifier = Modifier.weight(1f))
    }
}

enum class HomeTab(
    val title: String,
    val icon: Int,
    val unSelectedIcon: Int,
    val isGhost: Boolean = false,
) {
    HOME("Home", R.drawable.home_nav_selected, R.drawable.home_nav_unselected),
    GHOST_1("", 0, 0, true),
    GHOST_2("", 0, 0, true),
    ACCOUNT("Account", R.drawable.account_nav, R.drawable.account_nav),
}
