package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.http.CookieType
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
) : ViewModel() {
    private val coroutineScope = CoroutineScope(appDispatchers.io)
    private val _state = MutableStateFlow(AccountsState(getAccountInfo()))
    val state: StateFlow<AccountsState> = _state.asStateFlow()

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
            preferences.remove(CookieType.USER_IDENTITY.value)
            sessionManager.updateState(SessionState.Initial)
        }
    }
}

data class AccountsState(
    val accountInfo: AccountInfo?,
)

data class AccountInfo(
    val userPrincipal: String,
    val profilePic: String,
)
