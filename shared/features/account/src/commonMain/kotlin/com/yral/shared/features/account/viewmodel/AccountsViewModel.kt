package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import com.yral.shared.core.session.SessionManager
import com.yral.shared.uniffi.generated.propicFromPrincipal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountsViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {
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
}

data class AccountsState(
    val accountInfo: AccountInfo?,
)

data class AccountInfo(
    val userPrincipal: String,
    val profilePic: String,
)
