package com.yral.shared.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class SessionManager {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Initial)
    private val mutableProperties = MutableStateFlow(SessionProperties())

    val canisterID: String?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.canisterId
                else -> null
            }

    val userPrincipal: String?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.userPrincipal
                else -> null
            }

    val identity: ByteArray?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.identity
                else -> null
            }

    val profilePic: String?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.profilePic
                else -> null
            }

    val username: String?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.username
                else -> null
            }

    val isCreatedFromServiceCanister: Boolean?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.isCreatedFromServiceCanister
                else -> null
            }

    val profileVideosCount: Int
        get() = mutableProperties.value.profileVideosCount ?: 0

    fun updateState(state: SessionState) {
        mutableState.update { state }
        mutableProperties.update { SessionProperties() }
    }

    fun updateCoinBalance(newBalance: Long) {
        mutableProperties.update { it.copy(coinBalance = newBalance) }
    }

    fun updateSocialSignInStatus(isSocialSignIn: Boolean) {
        mutableProperties.update { it.copy(isSocialSignIn = isSocialSignIn) }
    }

    fun updateProfileVideosCount(count: Int?) {
        mutableProperties.update { it.copy(profileVideosCount = count) }
    }

    fun updateIsForcedGamePlayUser(isForcedGamePlayUser: Boolean) {
        mutableProperties.update { it.copy(isForcedGamePlayUser = isForcedGamePlayUser) }
    }

    fun updateLoggedInUserEmail(email: String?) {
        mutableProperties.update { it.copy(emailId = email) }
    }

    fun updateUsername(username: String?) {
        mutableState.update { state ->
            if (state is SessionState.SignedIn) {
                state.copy(session = state.session.copy(username = username))
            } else {
                state
            }
        }
    }

    fun updateFirebaseLoginState(isLoggedIn: Boolean) {
        mutableProperties.update { it.copy(isFirebaseLoggedIn = isLoggedIn) }
    }

    fun updateDailyRank(dailyRank: Long?) {
        mutableProperties.update { it.copy(dailyRank = dailyRank) }
    }

    fun addPrincipalToFollow(principal: String) {
        mutableProperties.update { it.copy(followedPrincipals = it.followedPrincipals + principal) }
        mutableProperties.update { it.copy(unFollowedPrincipals = it.unFollowedPrincipals - principal) }
    }

    fun removePrincipalFromFollow(principal: String) {
        mutableProperties.update { it.copy(followedPrincipals = it.followedPrincipals - principal) }
        mutableProperties.update { it.copy(unFollowedPrincipals = it.unFollowedPrincipals + principal) }
    }

    fun <T : Any> observeSessionPropertyWithDefault(
        selector: (SessionProperties) -> T?,
        defaultValue: T,
    ) = mutableProperties
        .map { selector(it) ?: defaultValue }
        .distinctUntilChanged()

    fun <T> observeSessionProperty(selector: (SessionProperties) -> T) =
        mutableProperties
            .map(selector)
            .distinctUntilChanged()

    fun <R> observeSessionStateWithProperty(transform: suspend (SessionState, SessionProperties) -> R) =
        combine(mutableState.asStateFlow(), mutableProperties.asStateFlow(), transform).distinctUntilChanged()

    fun <T> observeSessionState(transform: suspend (SessionState) -> T) =
        mutableState
            .map(transform)
            .distinctUntilChanged()

    fun resetSessionProperties() {
        mutableProperties.update {
            SessionProperties(
                coinBalance = 0,
                profileVideosCount = 0,
                isSocialSignIn = false,
            )
        }
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
        is SessionState.SignedIn -> "${SessionKey.SIGNED_IN.name}-${this.session.userPrincipal}"
        else -> SessionKey.INITIAL.name
    }

enum class SessionKey {
    SIGNED_IN,
    INITIAL,
}

const val DELAY_FOR_SESSION_PROPERTIES = 500L
