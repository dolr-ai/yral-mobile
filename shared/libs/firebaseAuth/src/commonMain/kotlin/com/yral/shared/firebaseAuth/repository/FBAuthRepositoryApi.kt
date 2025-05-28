package com.yral.shared.firebaseAuth.repository

import com.yral.shared.firebaseAuth.model.AuthState
import kotlinx.coroutines.flow.StateFlow

interface FBAuthRepositoryApi {
    /**
     * Observes the current authentication state
     */
    fun observeAuthState(): StateFlow<AuthState>

    /**
     * Signs in anonymously
     * @return Result containing the user ID if successful, or an exception if failed
     */
    suspend fun signInAnonymously(): Result<String>

    /**
     * Signs in with custom token
     * @return Result containing the user ID if successful, or an exception if failed
     */
    suspend fun signInWithToken(token: String): Result<String>

    /**
     * Signs out the current user
     */
    suspend fun signOut()

    /**
     * Gets the current user ID if authenticated, null otherwise
     */
    fun getCurrentUserId(): String?

    /**
     * Gets the current user's ID token if authenticated, null otherwise
     * Note: This returns the cached token. For a fresh token, use refreshIdToken()
     */
    suspend fun getIdToken(): String?

    /**
     * Refreshes and returns the current user's ID token
     * @return Result containing the ID token if successful, or an exception if failed
     */
    suspend fun refreshIdToken(): Result<String?>
}
