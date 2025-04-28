package com.yral.android.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.yral.android.R
import com.yral.shared.rust.domain.models.FeedDetails

@Composable
fun HomeScreen(
    feedDetails: List<FeedDetails>,
    isLoadingMore: Boolean,
    loadMoreFeed: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HomeTab.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                painter =
                                    if (selectedTab == tab) {
                                        painterResource(tab.icon)
                                    } else {
                                        painterResource(tab.unSelectedIcon)
                                    },
                                contentDescription = tab.title,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.HOME ->
                FeedScreen(
                    modifier = Modifier.padding(innerPadding),
                    feedDetails = feedDetails,
                    isLoadingMore = isLoadingMore,
                    loadMoreFeed = loadMoreFeed,
                )

            HomeTab.Account ->
                ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

enum class HomeTab(
    val title: String,
    val icon: Int,
    val unSelectedIcon: Int,
) {
    HOME("Home", R.drawable.home_nav_selected, R.drawable.home_nav_unselected),
    Account("Account", R.drawable.account_nav, R.drawable.account_nav),
}
