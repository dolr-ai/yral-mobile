package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.auth.AuthClient
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
) : ViewModel() {
    private val coroutineScope = CoroutineScope(appDispatchers.io)
    private val _state =
        MutableStateFlow(
            AccountsState(
                accountInfo = getAccountInfo(),
                isSocialSignInSuccessful = false,
            ),
        )
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
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
            preferences.remove(PrefKeys.IDENTITY_DATA.name)
            preferences.remove(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name)
            preferences.remove(PrefKeys.REFRESH_TOKEN.name)
            sessionManager.updateState(SessionState.Initial)
        }
    }

    fun signInWithGoogle() {
        coroutineScope
            .launch {
                authClient.signInWithSocial(
                    provider = SocialProvider.GOOGLE,
                )
            }
    }

    private suspend fun isSocialSignInSuccessful(): Boolean =
        preferences
            .getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false
}

data class AccountsState(
    val accountInfo: AccountInfo?,
    val isSocialSignInSuccessful: Boolean,
)

data class AccountInfo(
    val userPrincipal: String,
    val profilePic: String,
)
