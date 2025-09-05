package com.yral.android.ui.screens.leaderboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.android.ui.screens.leaderboard.details.LeaderboardDetailsScreen
import com.yral.android.ui.screens.leaderboard.main.LeaderboardMainScreen

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
                Box(modifier = Modifier.fillMaxSize()) {
                    LeaderboardMainScreen(component = instance.component, modifier = Modifier.fillMaxSize())
                    Button(
                        onClick = instance.component::openDailyHistory,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    ) { Text("View Details") }
                }
            }
            is LeaderboardComponent.Child.Details -> {
                LeaderboardDetailsScreen(component = instance.component)
            }
        }
    }
}
