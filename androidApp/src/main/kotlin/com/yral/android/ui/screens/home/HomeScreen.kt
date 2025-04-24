package com.yral.android.ui.screens.home

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.YralColors
import com.yral.shared.rust.domain.models.FeedDetails

@Composable
fun HomeScreen(
    feedDetails: List<FeedDetails>,
    currentPage: Int,
    onCurrentPageChange: (pageNo: Int) -> Unit,
    isLoadingMore: Boolean,
    loadMoreFeed: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HomeTab.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .height(67.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            NavBarIcon(
                                isSelected = selectedTab == tab,
                                tab = tab,
                            )
                        },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.White,
                                indicatorColor = Color.Transparent, // Remove the default indicator
                            ),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.HOME ->
                FeedScreen(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    feedDetails = feedDetails,
                    isLoadingMore = isLoadingMore,
                    loadMoreFeed = loadMoreFeed,
                    currentPage = currentPage,
                    onCurrentPageChange = onCurrentPageChange,
                )

            HomeTab.Account ->
                ProfileScreen(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.primaryContainer),
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
                                YralColors.navBarSelectionIndicationColor
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
) {
    HOME("Home", R.drawable.home_nav_selected, R.drawable.home_nav_unselected),
    Account("Account", R.drawable.account_nav, R.drawable.account_nav),
}
