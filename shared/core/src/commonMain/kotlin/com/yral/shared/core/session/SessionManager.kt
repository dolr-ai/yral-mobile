package com.yral.shared.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Suppress("TooManyFunctions")
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

    val bio: String?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.bio
                else -> null
            }

    val isCreatedFromServiceCanister: Boolean?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.isCreatedFromServiceCanister
                else -> null
            }

    val isBotAccount: Boolean?
        get() =
            when (val state = mutableState.value) {
                is SessionState.SignedIn -> state.session.isBotAccount
                else -> null
            }

    val profileVideosCount: Int
        get() = mutableProperties.value.profileVideosCount ?: 0

    fun updateState(state: SessionState) {
        mutableState.update { state }
        mutableProperties.update {
            // Preserve pending tournament registration across session changes
            SessionProperties(pendingTournamentRegistrationId = it.pendingTournamentRegistrationId)
        }
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

    fun updateIsAutoScrolledEnabled(isAutoScrollEnabled: Boolean) {
        mutableProperties.update { it.copy(isAutoScrollEnabled = isAutoScrollEnabled) }
    }

    fun updateIsMandatoryLogin(isMandatoryLogin: Boolean) {
        mutableProperties.update { it.copy(isMandatoryLogin = isMandatoryLogin) }
    }

    fun updateLoggedInUserEmail(email: String?) {
        mutableProperties.update { it.copy(emailId = email) }
    }

    fun updatePhoneNumber(phoneNumber: String?) {
        mutableProperties.update { it.copy(phoneNumber = phoneNumber) }
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

    fun updateProfilePicture(profilePictureUrl: String?) {
        mutableState.update { state ->
            if (state is SessionState.SignedIn) {
                state.copy(session = state.session.copy(profilePic = profilePictureUrl))
            } else {
                state
            }
        }
    }

    fun updateBio(bio: String?) {
        mutableState.update { state ->
            if (state is SessionState.SignedIn) {
                state.copy(session = state.session.copy(bio = bio))
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

    fun setPendingTournamentRegistrationId(tournamentId: String?) {
        mutableProperties.update { it.copy(pendingTournamentRegistrationId = tournamentId) }
    }

    fun consumePendingTournamentRegistrationId(): String? {
        val pending = mutableProperties.value.pendingTournamentRegistrationId
        if (pending != null) {
            mutableProperties.update { it.copy(pendingTournamentRegistrationId = null) }
        }
        return pending
    }

    fun addPrincipalToFollow(principal: String) {
        mutableProperties.update {
            it.copy(
                followedPrincipals = it.followedPrincipals + principal,
                unFollowedPrincipals = it.unFollowedPrincipals - principal,
            )
        }
    }

    fun removePrincipalFromFollow(principal: String) {
        mutableProperties.update {
            it.copy(
                followedPrincipals = it.followedPrincipals - principal,
                unFollowedPrincipals = it.unFollowedPrincipals + principal,
            )
        }
    }

    fun updateProDetails(details: ProDetails) {
        mutableProperties.update { it.copy(proDetails = details) }
    }

    fun clearProDetails() {
        mutableProperties.update { it.copy(proDetails = null) }
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

    suspend fun <T : Any> readLatestSessionPropertyWithDefault(
        selector: (SessionProperties) -> T?,
        defaultValue: T,
    ) = mutableProperties
        .map { selector(it) ?: defaultValue }
        .first()

    fun isCoinBalanceLoaded(): Boolean = mutableProperties.value.coinBalance != null

    fun isFirebaseLoggedIn(): Boolean = mutableProperties.value.isFirebaseLoggedIn

    fun resetSessionProperties() {
        mutableProperties.update {
            SessionProperties(
                coinBalance = 0,
                profileVideosCount = 0,
                isSocialSignIn = false,
                // Preserve pending tournament registration across session resets
                pendingTournamentRegistrationId = it.pendingTournamentRegistrationId,
            )
        }
    }
}

sealed interface SessionState {
    data object Initial : SessionState
    data object Loading : SessionState
    data class SignedIn(
        val session: Session,
    ) : SessionState {
        override fun toString(): String = "SignedIn ${session.userPrincipal ?: ""}"
    }
}

fun SessionState.getKey(): String =
    when (this) {
        is SessionState.SignedIn -> "${SessionKey.SIGNED_IN.name}-${this.session.userPrincipal}"
        else -> SessionKey.INITIAL.name
    }

fun SessionState.hasSameUserPrincipal(other: SessionState): Boolean =
    when {
        this::class != other::class -> false
        this is SessionState.SignedIn && other is SessionState.SignedIn -> {
            this.session.userPrincipal == other.session.userPrincipal
        }
        else -> true
    }

enum class SessionKey {
    SIGNED_IN,
    INITIAL,
}

const val DELAY_FOR_SESSION_PROPERTIES = 500L
