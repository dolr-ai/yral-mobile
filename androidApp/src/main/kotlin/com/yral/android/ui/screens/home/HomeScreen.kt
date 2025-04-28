package com.yral.android.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yral.shared.rust.domain.models.FeedDetails

@Composable
fun HomeScreen(
    feedDetails: List<FeedDetails>,
    isLoadingMore: Boolean,
    loadMoreFeed: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HomeTab.Feed) }

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
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.Feed ->
                FeedScreen(
                    modifier = Modifier.padding(innerPadding),
                    feedDetails = feedDetails,
                    isLoadingMore = isLoadingMore,
                    loadMoreFeed = loadMoreFeed,
                )

            HomeTab.Profile ->
                ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

enum class HomeTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Feed("Feed", Icons.Default.Home),
    Profile("Profile", Icons.Default.AccountCircle),
}
