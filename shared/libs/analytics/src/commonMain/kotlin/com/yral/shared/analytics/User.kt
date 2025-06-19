package com.yral.shared.analytics

enum class UserType {
    NEW,
    EXISTING,
}

enum class TokenType {
    CENTS,
    DOLR,
}

data class User(
    val userId: String,
    val name: String,
    val emailId: String,
    val canisterId: String,
    val isCreator: Boolean,
    val isNsfwEnabled: Boolean,
    val userType: UserType,
    val tokenWalletBalance: Double,
    val dolrWalletBalance: Double,
    val tokenType: TokenType,
)
