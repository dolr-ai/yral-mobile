package com.yral.shared.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionManager {
    private val _state = MutableStateFlow<SessionState>(SessionState.Initial)
    val state = _state.asStateFlow()

    private val sessionProperties = MutableStateFlow(SessionProperties())

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

    val username: String?
        get() =
            when (val state = _state.value) {
                is SessionState.SignedIn -> state.session.username
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
        sessionProperties.update { SessionProperties() }
    }

    fun updateCoinBalance(newBalance: Long) {
        sessionProperties.update { it.copy(coinBalance = newBalance) }
    }

    fun updateSocialSignInStatus(isSocialSignIn: Boolean) {
        sessionProperties.update { it.copy(isSocialSignIn = isSocialSignIn) }
    }

    fun updateProfileVideosCount(count: Int?) {
        sessionProperties.update { it.copy(profileVideosCount = count) }
    }

    fun updateIsForcedGamePlayUser(isForcedGamePlayUser: Boolean) {
        sessionProperties.update { it.copy(isForcedGamePlayUser = isForcedGamePlayUser) }
    }

    fun updateLoggedInUserEmail(email: String?) {
        sessionProperties.update { it.copy(emailId = email) }
    }

    fun updateUsername(username: String?) {
        _state.update { state ->
            if (state is SessionState.SignedIn) {
                state.copy(session = state.session.copy(username = username))
            } else {
                state
            }
        }
    }

    fun updateFirebaseLoginState(isLoggedIn: Boolean) {
        sessionProperties.update { it.copy(isFirebaseLoggedIn = isLoggedIn) }
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
