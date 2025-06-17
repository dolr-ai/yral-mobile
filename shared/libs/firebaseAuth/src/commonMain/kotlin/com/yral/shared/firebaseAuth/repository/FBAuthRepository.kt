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
    private val authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)

    override fun observeAuthState(): StateFlow<AuthState> = authState.asStateFlow()

    override suspend fun signInAnonymously(): Result<String> =
        try {
            authState.emit(AuthState.Loading)
            val result = auth.signInAnonymously()
            val user = result.user
            if (user == null) {
                Result.failure(YralException("Sign in successful but user is null"))
            } else {
                authState.emit(AuthState.Authenticated(user.uid))
                Result.success(user.uid)
            }
        } catch (e: FirebaseAuthException) {
            val errorMessage = e.message ?: "Unknown error during sign in"
            authState.emit(AuthState.Error(errorMessage))
            Result.failure(YralException(errorMessage))
        } catch (e: Exception) {
            val errorMessage = "Unexpected error during sign in: ${e.message}"
            authState.emit(AuthState.Error(errorMessage))
            Result.failure(YralException(errorMessage))
        }

    override suspend fun signInWithToken(token: String): Result<String> =
        try {
            authState.emit(AuthState.Loading)
            val result = auth.signInWithCustomToken(token)
            val user = result.user
            if (user == null) {
                Result.failure(YralException("Sign in successful but user is null"))
            } else {
                authState.emit(AuthState.Authenticated(user.uid))
                Result.success(user.uid)
            }
        } catch (e: FirebaseAuthException) {
            val errorMessage = e.message ?: "Unknown error during sign in"
            authState.emit(AuthState.Error(errorMessage))
            Result.failure(YralException(errorMessage))
        } catch (e: Exception) {
            val errorMessage = "Unexpected error during sign in: ${e.message}"
            authState.emit(AuthState.Error(errorMessage))
            Result.failure(YralException(errorMessage))
        }

    override suspend fun signOut() =
        try {
            auth.signOut()
            authState.emit(AuthState.NotAuthenticated)
        } catch (e: FirebaseAuthException) {
            val errorMessage = e.message ?: "Error during sign out"
            authState.emit(AuthState.Error(errorMessage))
            throw YralException(errorMessage)
        } catch (e: Exception) {
            val errorMessage = "Unexpected error during sign out: ${e.message}"
            authState.emit(AuthState.Error(errorMessage))
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
            val user = auth.currentUser
            if (user == null) {
                Result.failure(YralException("No user is currently signed in"))
            } else {
                val token = user.getIdToken(true)
                Result.success(token)
            }
        } catch (e: FirebaseAuthException) {
            Result.failure(YralException(e.message ?: "Error refreshing ID token"))
        } catch (e: Exception) {
            Result.failure(YralException("Unexpected error refreshing ID token: ${e.message}"))
        }

    init {
        // Initialize the auth state based on current user
        auth.currentUser?.let { user ->
            authState.tryEmit(AuthState.Authenticated(user.uid))
        }
    }
}
