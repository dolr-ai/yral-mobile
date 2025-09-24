package com.yral.shared.features.leaderboard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.leaderboard.ui.history.LeaderboardDetailsScreen
import com.yral.shared.features.leaderboard.ui.main.LeaderboardMainScreen

@Composable
fun LeaderboardScreen(
    component: LeaderboardComponent,
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
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is LeaderboardComponent.Child.Details -> {
                LeaderboardDetailsScreen(component = instance.component)
            }
        }
    }
}
