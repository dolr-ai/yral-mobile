package com.yral.shared.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionManager {
    private val _state = MutableStateFlow<SessionState>(SessionState.Initial)
    val state = _state.asStateFlow()

    private val _sessionProperties = MutableStateFlow(SessionProperties())
    val sessionProperties = _sessionProperties.asStateFlow()

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

    val profilePic: String?
        get() =
            when (val state = _state.value) {
                is SessionState.SignedIn -> state.session.profilePic
                else -> null
            }

    val isCreatedFromServiceCanister: Boolean?
        get() =
            when (val state = _state.value) {
                is SessionState.SignedIn -> state.session.isCreatedFromServiceCanister
                else -> null
            }

    fun updateState(state: SessionState) {
        _state.update { state }
        _sessionProperties.update { SessionProperties() }
    }

    fun updateCoinBalance(newBalance: Long) {
        _sessionProperties.update { it.copy(coinBalance = newBalance) }
    }

    fun updateSocialSignInStatus(isSocialSignIn: Boolean) {
        _sessionProperties.update { it.copy(isSocialSignIn = isSocialSignIn) }
    }

    fun updateProfileVideosCount(count: Int?) {
        _sessionProperties.update { it.copy(profileVideosCount = count) }
    }

    fun updateIsForcedGamePlayUser(isForcedGamePlayUser: Boolean) {
        _sessionProperties.update { it.copy(isForcedGamePlayUser = isForcedGamePlayUser) }
    }

    fun updateLoggedInUserEmail(email: String?) {
        _sessionProperties.update { it.copy(emailId = email) }
    }

    fun observeSessionProperties(): StateFlow<SessionProperties> = sessionProperties

    fun resetSessionProperties() {
        _sessionProperties.update {
            SessionProperties(
                coinBalance = 0,
                profileVideosCount = 0,
                isSocialSignIn = false,
            )
        }
    }

    fun isSocialSignIn(): Boolean = sessionProperties.value.isSocialSignIn ?: false

    fun profileVideosCount(): Int = sessionProperties.value.profileVideosCount ?: 0
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
        is SessionState.SignedIn -> "${SessionKey.SIGNED_IN.name}-${this.session.userPrincipal}"
        else -> SessionKey.INITIAL.name
    }

enum class SessionKey {
    SIGNED_IN,
    INITIAL,
}

const val DELAY_FOR_SESSION_PROPERTIES = 500L
