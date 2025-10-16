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
import com.yral.shared.features.wallet.domain.GetRewardConfigUseCase
import com.yral.shared.features.wallet.domain.GetUserBtcBalanceUseCase
import com.yral.shared.features.wallet.domain.models.BtcRewardConfig
import com.yral.shared.firebaseStore.getDownloadUrl
import dev.gitlive.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WalletViewModel(
    private val sessionManager: SessionManager,
    private val getBtcConversionUseCase: GetBtcConversionUseCase,
    private val getUserBtcBalanceUseCase: GetUserBtcBalanceUseCase,
    private val getRewardConfigUseCase: GetRewardConfigUseCase,
    private val walletTelemetry: WalletTelemetry,
    private val firebaseStorage: FirebaseStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    init {
        observeBalance()
        viewModelScope.launch {
            getRewardConfigUseCase
                .invoke()
                .onSuccess { rewardConfig -> _state.update { it.copy(rewardConfig = rewardConfig) } }
                .onFailure { Logger.e("Wallet") { "error fetching reward config $it" } }
        }
        viewModelScope.launch {
            val animation =
                getDownloadUrl(
                    path = BTC_REWARD_ANIMATION_URL,
                    storage = firebaseStorage,
                )
            if (animation.isNotEmpty()) {
                _state.update { it.copy(rewardAnimationUrl = animation) }
            }
        }
        viewModelScope.launch {
            sessionManager.observeSessionProperty({ it.isFirebaseLoggedIn }) { isFirebaseLoggedIn ->
                _state.update { it.copy(isFirebaseLoggedIn = true) }
            }
        }
    }

    fun onScreenViewed() {
        _state.value.accountInfo?.userPrincipal ?: return
        walletTelemetry.onWalletScreenViewed()
    }

    fun refresh(countryCode: String) {
        _state.update { it.copy(accountInfo = sessionManager.getAccountInfo()) }
        getUserBtcBalanceUseCase()
        if (_state.value.isFirebaseLoggedIn) {
            getBtcValueConversion(countryCode)
        }
    }

    private fun observeBalance() {
        viewModelScope.launch {
            sessionManager.observeSessionProperty({ it.coinBalance }) { coinBalance ->
                Logger.d("coinBalance") { "coin balance collected $coinBalance" }
                _state.update { it.copy(yralTokenBalance = coinBalance ?: 0) }
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

    fun toggleHowToEarnHelp(isOpen: Boolean) {
        _state.update { it.copy(howToEarnHelpVisible = isOpen) }
        if (isOpen) {
            walletTelemetry.onHowToEarnClicked()
        }
    }

    companion object {
        private const val BTC_REWARD_ANIMATION_URL = "btc_rewards/btc_rewards_views.json"
    }
}

data class WalletState(
    val yralTokenBalance: Long? = null,
    val bitcoinBalance: Double? = null,
    val btcConversionRate: Double? = null,
    val btcConversionCurrency: String? = null,
    val accountInfo: AccountInfo? = null,
    val howToEarnHelpVisible: Boolean = false,
    val rewardConfig: BtcRewardConfig? = null,
    val rewardAnimationUrl: String? = null,
    val isFirebaseLoggedIn: Boolean = false,
)
