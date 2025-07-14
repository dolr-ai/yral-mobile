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

    val canisterID: String?
        get() =
            when (val state = _state.value) {
                is SessionState.SignedIn -> state.session.canisterId
                else -> null
            }

    val userPrincipal: String?
        get() =
            when (val state = _state.value) {
                is SessionState.SignedIn -> state.session.userPrincipal
                else -> null
            }

    val identity: ByteArray?
        get() =
            when (val state = _state.value) {
                is SessionState.SignedIn -> state.session.identity
                else -> null
            }

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
