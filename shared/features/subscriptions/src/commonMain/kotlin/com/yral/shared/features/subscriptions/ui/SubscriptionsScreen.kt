package com.yral.shared.features.subscriptions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.core.session.ProDetails
import com.yral.shared.features.subscriptions.nav.SubscriptionsComponent
import com.yral.shared.features.subscriptions.viewmodel.SubscriptionScreenType
import com.yral.shared.features.subscriptions.viewmodel.SubscriptionViewModel
import com.yral.shared.iap.utils.getPurchaseContext
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.YralWebViewBottomSheet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun SubscriptionsScreen(
    component: SubscriptionsComponent,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    var linkToOpen by remember { mutableStateOf("") }
    val termsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Set entry point on first composition
    LaunchedEffect(component.entryPoint) {
        viewModel.setEntryPoint(component.entryPoint)
    }
    val viewState by viewModel.viewState.collectAsState()
    val purchaseContext = getPurchaseContext()
    val proDetails by viewModel.proDetails.collectAsStateWithLifecycle(ProDetails())
    val totalProCredits = proDetails.totalCredits
    val oldPrice = viewState.pricingInfo?.formattedOldPrice
    val newPrice = viewState.pricingInfo?.formattedCurrentPrice
    val tncUrl = viewState.tncUrl
    val privacyPolicyUrl = viewState.privacyPolicyUrl
    val onOpenLink = remember { { url: String -> linkToOpen = url } }
    when (val state = viewState.purchaseState) {
        is UiState.Initial,
        is UiState.InProgress,
        -> {
            Box(modifier = modifier) {
                SubscriptionInactiveScreen(
                    modifier = Modifier.fillMaxSize(),
                    creditsReceived = totalProCredits,
                    oldPrice = oldPrice,
                    newPrice = newPrice,
                    tncUrl = tncUrl,
                    privacyPolicyUrl = privacyPolicyUrl,
                    onBack = { component.onBack() },
                    onSubscribe = { purchaseContext?.let { viewModel.subscribe(it) } },
                    onOpenLink = onOpenLink,
                )
                if (state is UiState.InProgress) {
                    // Blocking loader
                    Box(
                        modifier = Modifier.fillMaxSize().clickable { },
                        contentAlignment = Alignment.Center,
                    ) { YralLoader() }
                }
            }
        }
        is UiState.Success -> {
            when (state.data) {
                is SubscriptionScreenType.UnPurchased -> {
                    SubscriptionInactiveScreen(
                        modifier = modifier,
                        creditsReceived = totalProCredits,
                        oldPrice = oldPrice,
                        newPrice = newPrice,
                        tncUrl = tncUrl,
                        privacyPolicyUrl = privacyPolicyUrl,
                        onBack = { component.onBack() },
                        onSubscribe = { purchaseContext?.let { viewModel.subscribe(it) } },
                        onOpenLink = onOpenLink,
                    )
                }
                is SubscriptionScreenType.Purchased -> {
                    val purchaseTimeMs = component.purchaseTimeMs
                    val billingPeriodMillis = viewState.billingPeriodMillis
                    val validTillMillis =
                        if (purchaseTimeMs != null && billingPeriodMillis != null && billingPeriodMillis > 0) {
                            purchaseTimeMs + billingPeriodMillis
                        } else {
                            null
                        }

                    SubscriptionActiveScreen(
                        modifier = modifier,
                        validTillText =
                            validTillMillis
                                ?.let { formatMillisWithOrdinal(it) }
                                ?: "",
                        creditsReceived = totalProCredits,
                        tncUrl = tncUrl,
                        privacyPolicyUrl = privacyPolicyUrl,
                        onBack = { component.onBack() },
                        onExploreHome = { component.onExploreFeed() },
                        onOpenLink = onOpenLink,
                    )
                }
                is SubscriptionScreenType.Success -> {
                    SubscriptionPaymentSuccessScreen(
                        modifier = modifier,
                        creditsReceived = totalProCredits,
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

    if (linkToOpen.isNotEmpty()) {
        YralWebViewBottomSheet(
            link = linkToOpen,
            bottomSheetState = termsSheetState,
            onDismissRequest = { linkToOpen = "" },
        )
    }
}

@Suppress("MagicNumber")
@OptIn(ExperimentalTime::class)
private fun formatMillisWithOrdinal(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date

    val day = localDate.day
    val suffix =
        when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }

    // Define the base format for Feb 2026
    val baseFormat =
        LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
        }

    // Combine manually: "15" + "th" + " " + "Feb 2026"
    return "${day}$suffix ${localDate.format(baseFormat)}"
}
