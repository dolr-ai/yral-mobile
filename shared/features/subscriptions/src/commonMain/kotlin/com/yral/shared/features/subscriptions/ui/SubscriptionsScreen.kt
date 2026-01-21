package com.yral.shared.features.subscriptions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.yral.shared.features.subscriptions.nav.SubscriptionsComponent
import com.yral.shared.features.subscriptions.nav.main.SubscriptionsMainComponent

@Composable
fun SubscriptionsScreen(
    component: SubscriptionsComponent,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        modifier = modifier,
    ) {
        when (val child = it.instance) {
            is SubscriptionsComponent.Child.Main ->
                SubscriptionsMainContent(
                    component = child.component,
                    modifier = modifier,
                )
        }
    }
}

@Composable
private fun SubscriptionsMainContent(
    component: SubscriptionsMainComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (component.showBackIcon) {
            Button(onClick = component.onBack) { Text("Back") }
        }
        Text(
            text = "Subscriptions",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Subscription UI coming soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
