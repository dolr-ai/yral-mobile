package com.yral.shared.features.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.wallet.analytics.WalletTelemetry
import com.yral.shared.features.wallet.domain.GetBillingBalanceUseCase
import com.yral.shared.features.wallet.domain.GetTransactionsUseCase
import com.yral.shared.features.wallet.domain.models.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WalletViewModel(
    private val sessionManager: SessionManager,
    private val walletTelemetry: WalletTelemetry,
    private val getBillingBalanceUseCase: GetBillingBalanceUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager
                .observeSessionProperty { it.isSocialSignIn }
                .collect { isSocialSignIn ->
                    _state.update { it.copy(isSocialSignedIn = isSocialSignIn == true) }
                    if (isSocialSignIn == true) {
                        loadWalletData()
                    }
                }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionProperty { it.botCount }
                .collect { botCount ->
                    _state.update { it.copy(hasBots = botCount != null && botCount > 0) }
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

    private fun loadWalletData() {
        val userPrincipal = sessionManager.userPrincipal ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getBillingBalanceUseCase(userPrincipal)
                .onSuccess { balance ->
                    _state.update {
                        it.copy(
                            totalEarningsInr = "₹${balance.balanceRupees}",
                            isLoading = false,
                        )
                    }
                }.onFailure {
                    _state.update { it.copy(totalEarningsInr = "₹0", isLoading = false) }
                }
        }
        viewModelScope.launch {
            _state.update { it.copy(isTransactionsLoading = true) }
            getTransactionsUseCase(userPrincipal)
                .onSuccess { transactions ->
                    _state.update {
                        it.copy(transactions = transactions, isTransactionsLoading = false)
                    }
                }.onFailure {
                    _state.update { it.copy(isTransactionsLoading = false) }
                }
        }
    }
}

data class WalletState(
    val totalEarningsInr: String = "",
    val isSocialSignedIn: Boolean = false,
    val hasBots: Boolean = false,
    val howToEarnVisible: Boolean = false,
    val showTransactionHistory: Boolean = false,
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val isTransactionsLoading: Boolean = false,
)
