package com.yral.shared.analytics

import com.yral.shared.analytics.events.TokenType

enum class UserType {
    NEW,
    EXISTING,
}

public data class User(
    val userId: String,
    val canisterId: String,
    val userType: UserType,
    val tokenWalletBalance: Double,
    val tokenType: TokenType,
)
