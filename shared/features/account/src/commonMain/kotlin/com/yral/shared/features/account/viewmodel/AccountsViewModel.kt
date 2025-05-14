package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.auth.utils.OAuthListener
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.uniffi.generated.propicFromPrincipal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountsViewModel(
    appDispatchers: AppDispatchers,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
    private val authClient: AuthClient,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel(),
    OAuthListener {
    private val coroutineScope = CoroutineScope(appDispatchers.io)
    private val _state =
        MutableStateFlow(
            AccountsState(
                accountInfo = getAccountInfo(),
                isSocialSignInSuccessful = false,
            ),
        )
    val state: StateFlow<AccountsState> = _state.asStateFlow()
    val sessionState = sessionManager.state

    init {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    isSocialSignInSuccessful = isSocialSignInSuccessful(),
                ),
            )
        }
    }

    fun refreshAccountInfo() {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    accountInfo = getAccountInfo(),
                    isSocialSignInSuccessful = isSocialSignInSuccessful(),
                ),
            )
        }
    }

    private fun getAccountInfo(): AccountInfo? {
        val canisterPrincipal = sessionManager.getCanisterPrincipal()
        val userPrincipal = sessionManager.getUserPrincipal()
        canisterPrincipal?.let { principal ->
            userPrincipal?.let { userPrincipal ->
                return AccountInfo(
                    profilePic = propicFromPrincipal(principal),
                    userPrincipal = userPrincipal,
                )
            }
        }
        return null
    }

    fun logout() {
        coroutineScope.launch {
            authClient.logout()
        }
    }

    fun signInWithGoogle() {
        val oAuthListener = this
        coroutineScope
            .launch {
                try {
                    authClient.signInWithSocial(
                        provider = SocialProvider.GOOGLE,
                        oAuthListener = oAuthListener,
                    )
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    crashlyticsManager.recordException(e)
                    setShowSignupFailedBottomSheet(true)
                }
            }
    }

    override fun setLoading(loading: Boolean) {
        showLoading(loading)
    }

    override fun exception(e: YralException) {
        showLoading(false)
        setShowSignupFailedBottomSheet(true)
    }

    private suspend fun isSocialSignInSuccessful(): Boolean =
        preferences
            .getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false

    fun showLoading(loading: Boolean) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    isLoading = loading,
                ),
            )
        }
    }

    fun setShowSignupFailedBottomSheet(show: Boolean) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    showSignupFailedBottomSheet = show,
                ),
            )
        }
    }
}

data class AccountsState(
    val accountInfo: AccountInfo?,
    val isSocialSignInSuccessful: Boolean,
    val isLoading: Boolean = false,
    val showSignupFailedBottomSheet: Boolean = false,
)

data class AccountInfo(
    val userPrincipal: String,
    val profilePic: String,
)
