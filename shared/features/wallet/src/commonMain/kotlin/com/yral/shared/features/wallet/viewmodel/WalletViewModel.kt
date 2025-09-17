package com.yral.shared.features.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.auth.utils.getAccountInfo
import com.yral.shared.features.wallet.analytics.WalletTelemetry
import com.yral.shared.features.wallet.domain.GetBtcInInrUseCase
import com.yral.shared.features.wallet.domain.GetUserBtcBalanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WalletViewModel(
    private val sessionManager: SessionManager,
    private val getBtcInInrUseCase: GetBtcInInrUseCase,
    private val getUserBtcBalanceUseCase: GetUserBtcBalanceUseCase,
    private val walletTelemetry: WalletTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    init {
        _state.update { it.copy(accountInfo = sessionManager.getAccountInfo()) }
        observeBalance()
        getBtcValueInInr()
        getUserBtcBalanceUseCase()
    }

    fun onScreenViewed() {
        _state.value.accountInfo?.userPrincipal ?: return
        walletTelemetry.onWalletScreenViewed()
    }

    fun refresh() {
        getBtcValueInInr()
        getUserBtcBalanceUseCase()
    }

    private fun observeBalance() {
        viewModelScope.launch {
            sessionManager.observeSessionProperties().collect { properties ->
                Logger.d("coinBalance") { "coin balance collected ${properties.coinBalance}" }
                properties.coinBalance?.let { balance ->
                    _state.update { it.copy(yralTokenBalance = balance) }
                }
            }
        }
    }

    private fun getBtcValueInInr() {
        viewModelScope.launch {
            getBtcInInrUseCase()
                .onSuccess { btcInInr ->
                    _state.update { it.copy(bitcoinValueInInr = btcInInr.priceInInr) }
                }
        }
    }

    private fun getUserBtcBalanceUseCase() {
        viewModelScope.launch {
            sessionManager.userPrincipal?.let { principal ->
                getUserBtcBalanceUseCase(principal)
                    .onSuccess { bal ->
                        Logger.d("coinBalance") { "btc balance collected $bal" }
                        _state.update { it.copy(bitcoinBalanceInSats = bal) }
                    }.onFailure {
                        Logger.d("coinBalance") { "error fetching btc balance $it" }
                    }
            }
        }
    }
}

data class WalletState(
    val yralTokenBalance: Long? = null,
    val bitcoinBalanceInSats: Double? = null,
    val bitcoinValueInInr: Double? = null,
    val accountInfo: AccountInfo? = null,
)
