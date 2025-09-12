package com.yral.shared.analytics

import com.yral.shared.analytics.events.TokenType

data class User(
    val userId: String,
    val canisterId: String,
    val isLoggedIn: Boolean?,
    val isCreator: Boolean?,
    val walletBalance: Double?,
    val tokenType: TokenType?,
    val isForcedGamePlayUser: Boolean?,
)
