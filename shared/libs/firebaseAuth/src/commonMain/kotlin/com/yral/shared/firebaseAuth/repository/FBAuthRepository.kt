package com.yral.shared.firebaseAuth.repository

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.firebaseAuth.model.AuthState
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("TooGenericExceptionCaught")
internal class FBAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
) : FBAuthRepositoryApi {
    val authState: StateFlow<AuthState>
        get() = _authState.asStateFlow()
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)

    override fun observeAuthState(): StateFlow<AuthState> = authState

    override suspend fun signInAnonymously(): Result<String> =
        try {
            _authState.emit(AuthState.Loading)
            val result = auth.signInAnonymously()
            val user = result.user ?: throw YralException("Sign in successful but user is null")
            _authState.emit(AuthState.Authenticated(user.uid))
            Result.success(user.uid)
        } catch (e: FirebaseAuthException) {
            val errorMessage = e.message ?: "Unknown error during sign in"
            _authState.emit(AuthState.Error(errorMessage))
            Result.failure(YralException(errorMessage))
        } catch (e: Exception) {
            val errorMessage = "Unexpected error during sign in: ${e.message}"
            _authState.emit(AuthState.Error(errorMessage))
            Result.failure(YralException(errorMessage))
        }

    override suspend fun signInWithToken(token: String): Result<String> =
        try {
            _authState.emit(AuthState.Loading)
            val result = auth.signInWithCustomToken(token)
            val user = result.user ?: throw YralException("Sign in successful but user is null")
            _authState.emit(AuthState.Authenticated(user.uid))
            Result.success(user.uid)
        } catch (e: FirebaseAuthException) {
            val errorMessage = e.message ?: "Unknown error during sign in"
            _authState.emit(AuthState.Error(errorMessage))
            Result.failure(YralException(errorMessage))
        } catch (e: Exception) {
            val errorMessage = "Unexpected error during sign in: ${e.message}"
            _authState.emit(AuthState.Error(errorMessage))
            Result.failure(YralException(errorMessage))
        }

    override suspend fun signOut() =
        try {
            auth.signOut()
            _authState.emit(AuthState.NotAuthenticated)
        } catch (e: FirebaseAuthException) {
            val errorMessage = e.message ?: "Error during sign out"
            _authState.emit(AuthState.Error(errorMessage))
            throw YralException(errorMessage)
        } catch (e: Exception) {
            val errorMessage = "Unexpected error during sign out: ${e.message}"
            _authState.emit(AuthState.Error(errorMessage))
            throw YralException(errorMessage)
        }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    @Suppress("SwallowedException")
    override suspend fun getIdToken(): String? =
        try {
            auth.currentUser?.getIdToken(false)
        } catch (e: FirebaseAuthException) {
            throw YralException(e.message ?: "Error getting ID token")
        }

    override suspend fun refreshIdToken(): Result<String?> =
        try {
            val user = auth.currentUser ?: throw YralException("No user is currently signed in")
            val token = user.getIdToken(true)
            Result.success(token)
        } catch (e: FirebaseAuthException) {
            Result.failure(YralException(e.message ?: "Error refreshing ID token"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error refreshing ID token: ${e.message}"))
        }

    init {
        // Initialize the auth state based on current user
        auth.currentUser?.let { user ->
            _authState.tryEmit(AuthState.Authenticated(user.uid))
        }
    }
}
