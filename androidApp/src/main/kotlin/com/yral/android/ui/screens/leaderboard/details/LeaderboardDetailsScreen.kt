package com.yral.android.ui.screens.leaderboard.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LeaderboardDetailsScreen(component: LeaderboardDetailsComponent) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Leaderboard Details")
        Button(onClick = component::onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Back")
        }
    }
}
