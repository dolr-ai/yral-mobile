package com.yral.shared.features.subscriptions.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.yral.shared.core.session.ProDetails
import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.utils.PurchaseContext
import com.yral.shared.libs.arch.presentation.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val iapManager: IAPManager,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _purchaseState =
        MutableStateFlow<UiState<SubscriptionScreenType>>(
            UiState.Initial,
        )
    val purchaseState: StateFlow<UiState<SubscriptionScreenType>> = _purchaseState.asStateFlow()

    val proDetails =
        sessionManager
            .observeSessionProperty { it.proDetails }

    init {
        // Observe proDetails and update state accordingly
        viewModelScope.launch {
            proDetails
                .collect { proDetails ->
                    // Only update if not in a transient state (InProgress or Success screen type)
                    val screenType = getScreenType(proDetails)
                    when (val currentState = _purchaseState.value) {
                        is UiState.Initial -> {
                            // Update based on proDetails
                            _purchaseState.update { UiState.Success(screenType) }
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
                                    _purchaseState.update { UiState.Success(screenType) }
                                }
                            }
                        }
                        is UiState.Failure -> {
                            // Update based on proDetails after failure
                            _purchaseState.update { UiState.Success(screenType) }
                        }
                    }
                }
        }
    }

    fun clearTransientState(proDetails: ProDetails?) {
        val screenType = getScreenType(proDetails)
        when (val currentState = _purchaseState.value) {
            is UiState.Success -> {
                // Handle different screen types
                when (currentState.data) {
                    is SubscriptionScreenType.Success -> {
                        _purchaseState.update { UiState.Success(screenType) }
                    }
                    is SubscriptionScreenType.Failure -> {
                        _purchaseState.update { UiState.Success(screenType) }
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

    fun subscribe(purchaseContext: PurchaseContext) {
        viewModelScope.launch {
            _purchaseState.update { UiState.InProgress() }
            iapManager
                .purchaseProduct(
                    productId = ProductId.YRAL_PRO,
                    context = purchaseContext,
                    acknowledgePurchase = false,
                ).onSuccess { purchase ->
                    Logger.d("SubscriptionViewModel") { "Purchase successful: $purchase" }
                    sessionManager.clearProDetails()
                    _purchaseState.update { UiState.Success(SubscriptionScreenType.Success) }
                }.onFailure { error ->
                    val iapError = error as? IAPError ?: IAPError.UnknownError(error)
                    Logger.e("SubscriptionViewModel", error) { "Purchase failed: $iapError" }
                    _purchaseState.update { UiState.Success(SubscriptionScreenType.Failure(iapError)) }
                }
        }
    }
}

sealed class SubscriptionScreenType {
    data object UnPurchased : SubscriptionScreenType()
    data object Purchased : SubscriptionScreenType()
    data object Success : SubscriptionScreenType()
    data class Failure(
        val error: IAPError,
    ) : SubscriptionScreenType()
}
