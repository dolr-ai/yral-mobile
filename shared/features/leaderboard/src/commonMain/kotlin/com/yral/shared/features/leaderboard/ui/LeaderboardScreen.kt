package com.yral.shared.features.leaderboard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.leaderboard.ui.history.LeaderboardDetailsScreen
import com.yral.shared.features.leaderboard.ui.main.LeaderboardMainScreen
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    component: LeaderboardComponent,
    leaderBoardViewModel: LeaderBoardViewModel,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        animation = stackAnimation(slide()),
        modifier = modifier,
    ) { child ->
        when (val instance = child.instance) {
            is LeaderboardComponent.Child.Main -> {
                LeaderboardMainScreen(
                    component = instance.component,
                    viewModel = leaderBoardViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is LeaderboardComponent.Child.Details -> {
                LeaderboardDetailsScreen(component = instance.component)
            }
        }
    }
}
