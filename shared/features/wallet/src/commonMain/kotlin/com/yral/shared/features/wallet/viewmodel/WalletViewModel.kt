package com.yral.shared.features.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.wallet.analytics.WalletTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WalletViewModel(
    private val sessionManager: SessionManager,
    private val walletTelemetry: WalletTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager
                .observeSessionProperty { it.isFirebaseLoggedIn }
                .collect { isFirebaseLoggedIn ->
                    _state.update { it.copy(isFirebaseLoggedIn = isFirebaseLoggedIn) }
                }
        }
    }

    fun onScreenViewed() {
        walletTelemetry.onWalletScreenViewed()
    }

    fun toggleHowToEarnHelp(isOpen: Boolean) {
        _state.update { it.copy(howToEarnVisible = isOpen) }
        if (isOpen) {
            walletTelemetry.onHowToEarnClicked()
        }
    }

    fun toggleTransactionHistory(show: Boolean) {
        _state.update { it.copy(showTransactionHistory = show) }
    }
}

data class WalletState(
    val totalEarningsInr: String = "₹800",
    val isFirebaseLoggedIn: Boolean = false,
    val howToEarnVisible: Boolean = false,
    val showTransactionHistory: Boolean = false,
)
