package com.yral.shared.features.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.utils.getAccountInfo
import com.yral.shared.features.wallet.analytics.WalletTelemetry
import com.yral.shared.features.wallet.domain.GetBtcConversionUseCase
import com.yral.shared.features.wallet.domain.GetUserBtcBalanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WalletViewModel(
    private val sessionManager: SessionManager,
    private val getBtcConversionUseCase: GetBtcConversionUseCase,
    private val getUserBtcBalanceUseCase: GetUserBtcBalanceUseCase,
    private val walletTelemetry: WalletTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    val firebaseLogin =
        sessionManager
            .observeSessionProperties()
            .map { it.isFirebaseLoggedIn }
            .distinctUntilChanged()

    init {
        observeBalance()
    }

    fun onScreenViewed() {
        _state.value.accountInfo?.userPrincipal ?: return
        walletTelemetry.onWalletScreenViewed()
    }

    fun refresh(
        countryCode: String,
        isFirebaseLoggedIn: Boolean,
    ) {
        _state.update { it.copy(accountInfo = sessionManager.getAccountInfo()) }
        getUserBtcBalanceUseCase()
        if (isFirebaseLoggedIn) {
            getBtcValueConversion(countryCode)
        }
    }

    private fun observeBalance() {
        viewModelScope.launch {
            sessionManager.observeSessionProperties().collect { properties ->
                Logger.d("coinBalance") { "coin balance collected ${properties.coinBalance}" }
                _state.update { it.copy(yralTokenBalance = properties.coinBalance ?: 0) }
            }
        }
    }

    private fun getBtcValueConversion(countryCode: String) {
        viewModelScope.launch {
            getBtcConversionUseCase(parameter = GetBtcConversionUseCase.Params(countryCode))
                .onSuccess { btcInInr ->
                    _state.update {
                        it.copy(
                            btcConversionRate = btcInInr.conversionRate,
                            btcConversionCurrency = btcInInr.currencyCode,
                        )
                    }
                }
        }
    }

    private fun getUserBtcBalanceUseCase() {
        viewModelScope.launch {
            sessionManager.userPrincipal?.let { principal ->
                getUserBtcBalanceUseCase(principal)
                    .onSuccess { bal ->
                        Logger.d("coinBalance") { "btc balance collected $bal" }
                        _state.update { it.copy(bitcoinBalance = bal) }
                    }.onFailure {
                        Logger.d("coinBalance") { "error fetching btc balance $it" }
                    }
            }
        }
    }

    fun toggleHowToEarnHelp() {
        _state.update { it.copy(howToEarnHelpVisible = !it.howToEarnHelpVisible) }
    }
}

data class WalletState(
    val yralTokenBalance: Long? = null,
    val bitcoinBalance: Double? = null,
    val btcConversionRate: Double? = null,
    val btcConversionCurrency: String? = null,
    val accountInfo: AccountInfo? = null,
    val howToEarnHelpVisible: Boolean = false,
)
