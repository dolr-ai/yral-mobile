package com.yral.shared.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionManager {
    private val _state = MutableStateFlow<SessionState>(SessionState.Initial)
    val state = _state.asStateFlow()

    private val _coinBalance = MutableStateFlow<Long>(0)
    val coinBalance: StateFlow<Long> = _coinBalance.asStateFlow()

    fun updateState(state: SessionState) {
        _state.update { state }
    }

    fun updateCoinBalance(newBalance: Long) {
        _coinBalance.update { newBalance }
    }

    fun getCanisterPrincipal(): String? =
        if (_state.value is SessionState.SignedIn) {
            (_state.value as SessionState.SignedIn).session.canisterPrincipal
        } else {
            null
        }

    fun getUserPrincipal(): String? =
        if (_state.value is SessionState.SignedIn) {
            (_state.value as SessionState.SignedIn).session.userPrincipal
        } else {
            null
        }

    fun getIdentity(): ByteArray? =
        if (_state.value is SessionState.SignedIn) {
            (_state.value as SessionState.SignedIn).session.identity
        } else {
            null
        }
}

sealed interface SessionState {
    data object Initial : SessionState
    data object Loading : SessionState
    data class SignedIn(
        val session: Session,
    ) : SessionState
}

fun SessionState.getKey(): String =
    when (this) {
        is SessionState.SignedIn -> "signed-${this.session.userPrincipal}"
        else -> "anon"
    }
