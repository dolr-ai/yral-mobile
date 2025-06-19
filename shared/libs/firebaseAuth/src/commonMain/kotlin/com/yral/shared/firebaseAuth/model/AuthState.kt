package com.yral.shared.firebaseAuth.model

sealed class AuthState {
    data object Loading : AuthState()
    data object NotAuthenticated : AuthState()
    data class Authenticated(
        val userId: String,
    ) : AuthState()
    data class Error(
        val message: String,
    ) : AuthState()
}
