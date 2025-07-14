package com.yral.shared.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionManager {
    private val _state = MutableStateFlow<SessionState>(SessionState.Initial)
    val state = _state.asStateFlow()

    private val sessionProperties =
        MutableStateFlow(
            value =
                SessionProperties(
                    coinBalance = 0,
                    profileVideosCount = 0,
                    isSocialSignIn = false,
                ),
        )

    fun updateState(state: SessionState) {
        _state.update { state }
    }

    fun updateCoinBalance(newBalance: Long) {
        sessionProperties.update { it.copy(coinBalance = newBalance) }
    }

    fun updateSocialSignInStatus(isSocialSignIn: Boolean) {
        sessionProperties.update { it.copy(isSocialSignIn = isSocialSignIn) }
    }

    fun updateProfileVideosCount(count: Int) {
        sessionProperties.update { it.copy(profileVideosCount = count) }
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

    fun observeSessionProperties(): StateFlow<SessionProperties> = sessionProperties.asStateFlow()

    fun resetSessionProperties() {
        sessionProperties.update {
            SessionProperties(
                coinBalance = 0,
                profileVideosCount = 0,
                isSocialSignIn = false,
            )
        }
    }

    fun isSocialSignIn(): Boolean = sessionProperties.value.isSocialSignIn

    fun profileVideosCount(): Int = sessionProperties.value.profileVideosCount
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
