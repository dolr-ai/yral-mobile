package com.yral.shared.features.subscriptions.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.yral.shared.features.subscriptions.nav.SubscriptionsComponent
import com.yral.shared.features.subscriptions.viewmodel.SubscriptionScreenType
import com.yral.shared.features.subscriptions.viewmodel.SubscriptionViewModel
import com.yral.shared.iap.utils.getPurchaseContext
import com.yral.shared.libs.arch.presentation.UiState
import org.koin.compose.viewmodel.koinViewModel

@Suppress("LongMethod")
@Composable
fun SubscriptionsScreen(
    component: SubscriptionsComponent,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val purchaseState by viewModel.purchaseState.collectAsState()
    val purchaseContext = getPurchaseContext()
    val proDetails by viewModel.proDetails.collectAsState(null)
    when (val state = purchaseState) {
        is UiState.Initial -> {
            SubscriptionInactiveScreen(
                modifier = modifier,
                onBack = { component.onBack() },
                onSubscribe = { purchaseContext?.let { viewModel.subscribe(it) } },
            )
        }
        is UiState.InProgress -> {
            SubscriptionInactiveScreen(
                modifier = modifier,
                onBack = { component.onBack() },
                onSubscribe = { }, // Disable during purchase
            )
        }
        is UiState.Success -> {
            when (state.data) {
                is SubscriptionScreenType.UnPurchased -> {
                    SubscriptionInactiveScreen(
                        modifier = modifier,
                        onBack = { component.onBack() },
                        onSubscribe = { purchaseContext?.let { viewModel.subscribe(it) } },
                    )
                }
                is SubscriptionScreenType.Purchased -> {
                    SubscriptionActiveScreen(
                        modifier = modifier,
                        validTillText = "Active",
                        onBack = { component.onBack() },
                        onExploreHome = { component.onExploreFeed() },
                    )
                }
                is SubscriptionScreenType.Success -> {
                    SubscriptionPaymentSuccessScreen(
                        modifier = modifier,
                        onClose = {
                            viewModel.clearTransientState(proDetails)
                            component.onBack()
                        },
                        onCreateVideo = {
                            viewModel.clearTransientState(proDetails)
                            component.onCreateVideo()
                        },
                        onExploreFeed = {
                            viewModel.clearTransientState(proDetails)
                            component.onExploreFeed()
                        },
                    )
                }
                is SubscriptionScreenType.Failure -> {
                    SubscriptionPaymentFailureScreen(
                        modifier = modifier,
                        onClose = {
                            viewModel.clearTransientState(proDetails)
                            component.onBack()
                        },
                        onTryAgain = { purchaseContext?.let { viewModel.subscribe(it) } },
                    )
                }
            }
        }
        is UiState.Failure -> {
            SubscriptionPaymentFailureScreen(
                modifier = modifier,
                onClose = { component.onBack() },
                onTryAgain = { purchaseContext?.let { viewModel.subscribe(it) } },
            )
        }
    }
}
