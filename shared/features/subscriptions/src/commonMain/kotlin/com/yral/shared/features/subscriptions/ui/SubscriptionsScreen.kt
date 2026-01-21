package com.yral.shared.features.subscriptions.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.yral.shared.features.subscriptions.nav.SubscriptionsComponent
import com.yral.shared.features.subscriptions.nav.main.SubscriptionsMainComponent
import com.yral.shared.features.subscriptions.ui.SubscriptionPaymentSuccessScreen

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
    SubscriptionPaymentSuccessScreen(
        modifier = modifier,
        onClose = component.onBack,
    )
}
