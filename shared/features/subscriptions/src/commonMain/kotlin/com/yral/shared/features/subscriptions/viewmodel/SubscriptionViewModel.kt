package com.yral.shared.features.subscriptions.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.ProDetails
import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.utils.PurchaseContext
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.rust.service.domain.models.SubscriptionPlan
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7Params
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7UseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val iapManager: IAPManager,
    private val sessionManager: SessionManager,
    private val getUserProfileDetailsV7UseCase: GetUserProfileDetailsV7UseCase,
) : ViewModel() {
    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    val proDetails =
        sessionManager
            .observeSessionPropertyWithDefault(
                selector = { it.proDetails },
                defaultValue = ProDetails(),
            )

    init {
        fetchProductDetails()
        // Observe proDetails and update state accordingly
        viewModelScope.launch {
            proDetails
                .collect { proDetails ->
                    // Only update if not in a transient state (InProgress or Success screen type)
                    val screenType = getScreenType(proDetails)
                    when (val currentState = _viewState.value.purchaseState) {
                        is UiState.Initial -> {
                            // Update based on proDetails
                            _viewState.update { it.copy(purchaseState = UiState.Success(screenType)) }
                        }
                        is UiState.InProgress -> {
                            // Don't update during purchase flow
                        }
                        is UiState.Success -> {
                            // Handle different screen types
                            when (currentState.data) {
                                is SubscriptionScreenType.Success,
                                is SubscriptionScreenType.Failure,
                                -> {
                                    // Keep failure state, don't update
                                }
                                is SubscriptionScreenType.UnPurchased,
                                is SubscriptionScreenType.Purchased,
                                -> {
                                    // Update based on proDetails for normal states
                                    _viewState.update {
                                        it.copy(purchaseState = UiState.Success(screenType))
                                    }
                                }
                            }
                        }
                        is UiState.Failure -> {
                            // Update based on proDetails after failure
                            _viewState.update {
                                it.copy(purchaseState = UiState.Success(screenType))
                            }
                        }
                    }
                }
        }
    }

    fun clearTransientState(proDetails: ProDetails?) {
        val screenType = getScreenType(proDetails)
        when (val currentState = _viewState.value.purchaseState) {
            is UiState.Success -> {
                // Handle different screen types
                when (currentState.data) {
                    is SubscriptionScreenType.Success -> {
                        _viewState.update {
                            it.copy(purchaseState = UiState.Success(screenType))
                        }
                    }
                    is SubscriptionScreenType.Failure -> {
                        _viewState.update {
                            it.copy(purchaseState = UiState.Success(screenType))
                        }
                    }
                    else -> {
                        // No need to clear for other screen types
                    }
                }
            }
            else -> {
                // No need to clear for non-success states
            }
        }
    }

    private fun getScreenType(proDetails: ProDetails?) =
        if (proDetails?.isProPurchased == true) {
            SubscriptionScreenType.Purchased
        } else {
            SubscriptionScreenType.UnPurchased
        }

    private fun fetchProductDetails() {
        viewModelScope.launch {
            iapManager
                .fetchProducts(listOf(ProductId.YRAL_PRO))
                .onSuccess { products ->
                    val product = products.firstOrNull()
                    product?.let { p ->
                        _viewState.update { current ->
                            val oldPrice = p.priceAmountMicros / CURRENCY_DIVIDER
                            val currentPrice = p.offerPriceAmountMicros / CURRENCY_DIVIDER
                            current.copy(
                                pricingInfo =
                                    PricingInfo(
                                        currentPrice = currentPrice,
                                        formattedCurrentPrice = p.offerPrice,
                                        oldPrice = oldPrice,
                                        formattedOldPrice = p.price,
                                        currencyCode = p.currencyCode,
                                    ),
                                billingPeriodMillis = p.billingPeriodMillis,
                            )
                        }
                    }
                }.onFailure { error ->
                    Logger.e("SubscriptionViewModel", error) { "Failed to fetch product details" }
                    _viewState.update { it.copy(pricingInfo = null) }
                }
        }
    }

    fun subscribe(purchaseContext: PurchaseContext) {
        viewModelScope.launch {
            _viewState.update { it.copy(purchaseState = UiState.InProgress()) }
            iapManager
                .purchaseProduct(
                    productId = ProductId.YRAL_PRO,
                    context = purchaseContext,
                    acknowledgePurchase = false,
                ).onSuccess { purchase ->
                    Logger.d("SubscriptionViewModel") { "Purchase successful: $purchase" }
                    sessionManager.clearProDetails()
                    viewModelScope.launch {
                        val principal = sessionManager.userPrincipal ?: return@launch
                        getUserProfileDetailsV7UseCase(
                            GetUserProfileDetailsV7Params(
                                principal = principal,
                                targetPrincipal = principal,
                            ),
                        ).onSuccess { details ->
                            val proPlan = details.subscriptionPlan as? SubscriptionPlan.Pro
                            proPlan?.let {
                                sessionManager.updateProDetails(
                                    details =
                                        ProDetails(
                                            isProPurchased = true,
                                            availableCredits = proPlan.subscription.freeVideoCreditsLeft.toInt(),
                                            totalCredits = proPlan.subscription.totalVideoCreditsAlloted.toInt(),
                                        ),
                                )
                            }
                            Logger.d("SubscriptionX") { "Updated pro details $proPlan" }
                        }.onFailure {
                            Logger.e("SubscriptionX", it) { "Failed to update pro details" }
                        }
                    }
                    _viewState.update {
                        it.copy(purchaseState = UiState.Success(SubscriptionScreenType.Success))
                    }
                }.onFailure { error ->
                    val iapError = error as? IAPError ?: IAPError.UnknownError(error)
                    Logger.e("SubscriptionViewModel", error) { "Purchase failed: $iapError" }
                    _viewState.update {
                        it.copy(purchaseState = UiState.Success(SubscriptionScreenType.Failure(iapError)))
                    }
                }
        }
    }

    companion object {
        private const val CURRENCY_DIVIDER = 1_000_000.0
    }
}

data class ViewState(
    val purchaseState: UiState<SubscriptionScreenType> = UiState.Initial,
    val pricingInfo: PricingInfo? = null,
    val billingPeriodMillis: Long? = null,
)

sealed class SubscriptionScreenType {
    data object UnPurchased : SubscriptionScreenType()
    data object Purchased : SubscriptionScreenType()
    data object Success : SubscriptionScreenType()
    data class Failure(
        val error: IAPError,
    ) : SubscriptionScreenType()
}

data class PricingInfo(
    val currentPrice: Double,
    val formattedCurrentPrice: String,
    val oldPrice: Double,
    val formattedOldPrice: String,
    val currencyCode: String,
)
